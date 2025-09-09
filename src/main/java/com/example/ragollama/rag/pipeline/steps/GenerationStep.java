package com.example.ragollama.rag.pipeline.steps;

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
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Order(40) // Выполняется после Augmentation
@RequiredArgsConstructor
@Slf4j
public class GenerationStep implements RagPipelineStep {

    private final LlmClient llmClient;
    private final NoContextStrategy noContextStrategy;
    private final ObjectMapper objectMapper;
    private final PiiRedactionService piiRedactionService;
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[([\\w\\-.:]+)]");

    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [40] Generation: вызов LLM с Chain-of-Thought...");
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

    private ChainOfThoughtResponse parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ChainOfThoughtResponse.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Generation LLM вернул невалидный JSON.", e);
        }
    }

    /**
     * Новая, более надежная логика извлечения цитат.
     */
    private List<SourceCitation> extractCitationsFromAnswer(String rawAnswer, List<Document> contextDocs) {
        Map<String, Document> docMap = contextDocs.stream()
                .collect(Collectors.toMap(doc -> (String) doc.getMetadata().get("chunkId"), Function.identity()));
        Matcher matcher = CITATION_PATTERN.matcher(rawAnswer);
        return matcher.results()
                .map(matchResult -> matchResult.group(1))
                .distinct()
                .map(docMap::get)
                .filter(doc -> doc != null)
                .map(this::toSourceCitation)
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
