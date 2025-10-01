package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.generation.NoContextStrategy;
import com.example.ragollama.rag.domain.model.ChainOfThoughtResponse;
import com.example.ragollama.rag.domain.model.RagAnswer;
import com.example.ragollama.rag.domain.model.SourceCitation;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.exception.GenerationException;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.processing.PiiRedactionService;
import com.example.ragollama.shared.task.TaskLifecycleService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Шаг RAG-конвейера, отвечающий за финальную генерацию ответа с использованием LLM.
 */
@Component
@Order(40)
@RequiredArgsConstructor
@Slf4j
public class GenerationStep implements RagPipelineStep {

    private final LlmClient llmClient;
    private final NoContextStrategy noContextStrategy;
    private final ObjectMapper objectMapper;
    private final PiiRedactionService piiRedactionService;
    private final TaskLifecycleService taskLifecycleService;
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[([\\w\\-.:]+)]");

    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [40] Generation: вызов LLM с Chain-of-Thought для полного ответа...");
        taskLifecycleService.getActiveTaskForSession(context.sessionId()).ifPresent(task ->
                taskLifecycleService.emitEvent(task.getId(), new UniversalResponse.StatusUpdate("Формулирую ответ...")));
        if (context.rerankedDocuments().isEmpty()) {
            return noContextStrategy.handle(context.finalPrompt()).map(context::withFinalAnswer);
        }
        return Mono.fromFuture(llmClient.callChat(context.finalPrompt(), ModelCapability.BALANCED, true))
                .map(jsonResponse -> {
                    ChainOfThoughtResponse cotResponse = parseLlmResponse(jsonResponse);
                    String rawAnswer = cotResponse.finalAnswer();
                    List<SourceCitation> citations = extractCitationsFromAnswer(rawAnswer, context.rerankedDocuments());
                    String cleanedAnswer = CITATION_PATTERN.matcher(rawAnswer).replaceAll("").trim();
                    RagAnswer finalAnswer = new RagAnswer(cleanedAnswer, citations);
                    return context.withFinalAnswer(finalAnswer);
                })
                .doOnError(ex -> log.error("Ошибка на этапе генерации ответа LLM", ex))
                .onErrorMap(ex -> new GenerationException("Не удалось сгенерировать ответ от LLM.", ex));
    }

    public Flux<StreamingResponsePart> generateStructuredStream(Prompt prompt, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("В потоковом запросе на этап Generation не передано документов.");
            return noContextStrategy.handle(prompt)
                    .flatMapMany(answer -> Flux.just(
                            new StreamingResponsePart.Content(answer.answer()),
                            new StreamingResponsePart.Done("Завершено без контекста")
                    ));
        }

        Flux<StreamingResponsePart> contentStream = llmClient.streamChat(prompt, ModelCapability.BALANCED)
                .map(StreamingResponsePart.Content::new);

        Flux<StreamingResponsePart> tailStream = Flux.just(
                new StreamingResponsePart.Sources(extractCitations(documents)),
                new StreamingResponsePart.Done("Успешно завершено")
        );

        return Flux.concat(contentStream, tailStream)
                .doOnError(ex -> log.error("Ошибка в потоке генерации ответа LLM", ex))
                .onErrorResume(ex -> {
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    if (cause instanceof CancellationException || cause instanceof IOException) {
                        log.warn("Поток RAG-генерации был прерван клиентом: {}", cause.getMessage());
                        return Flux.empty();
                    }
                    String errorMessage = "Ошибка при генерации ответа: " + ex.getMessage();
                    return Flux.just(new StreamingResponsePart.Error(errorMessage));
                });
    }

    private ChainOfThoughtResponse parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ChainOfThoughtResponse.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Generation LLM вернул невалидный JSON.", e);
        }
    }

    private List<SourceCitation> extractCitationsFromAnswer(String rawAnswer, List<Document> contextDocs) {
        Map<String, Document> docMap = contextDocs.stream()
                .collect(Collectors.toMap(doc -> (String) doc.getMetadata().get("chunkId"), Function.identity(), (d1, d2) -> d1));
        Matcher matcher = CITATION_PATTERN.matcher(rawAnswer);
        return matcher.results()
                .map(matchResult -> matchResult.group(1))
                .distinct()
                .map(docMap::get)
                .filter(doc -> doc != null)
                .map(this::toSourceCitation)
                .collect(Collectors.toList());
    }

    private List<SourceCitation> extractCitations(List<Document> documents) {
        if (documents == null) {
            return Collections.emptyList();
        }
        return documents.stream()
                .map(this::toSourceCitation)
                .distinct()
                .collect(Collectors.toList());
    }

    private SourceCitation toSourceCitation(Document doc) {
        return new SourceCitation(
                (String) doc.getMetadata().get("source"),
                piiRedactionService.redact(doc.getText()),
                doc.getMetadata(),
                (String) doc.getMetadata().get("chunkId"),
                (Float) doc.getMetadata().get("rerankedSimilarity")
        );
    }
}
