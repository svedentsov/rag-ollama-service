package com.example.ragollama.buganalysis.domain;

import com.example.ragollama.buganalysis.api.dto.BugAnalysisResponse;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Сервис-оркестратор для агента анализа баг-репортов.
 * <p>
 * Эта версия использует усиленный промпт и надежный внутренний парсер
 * для получения структурированного ответа от LLM без использования
 * внешних зависимостей для парсинга.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BugAnalysisService {

    private final QueryProcessingPipeline queryProcessingPipeline;
    private final HybridRetrievalStrategy retrievalStrategy;
    private final RetrievalProperties retrievalProperties;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    public CompletableFuture<BugAnalysisResponse> analyzeBugReport(String draftDescription) {
        log.info("Запуск анализа бага для: '{}'", draftDescription);

        Filter.Expression filter = new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key("metadata.doc_type"), new Filter.Value("bug_report"));
        var retrievalConfig = retrievalProperties.hybrid().vectorSearch();

        return queryProcessingPipeline.process(draftDescription)
                .flatMap(processedQueries -> retrievalStrategy.retrieve(
                        processedQueries,
                        draftDescription,
                        retrievalConfig.topK(),
                        retrievalConfig.similarityThreshold(),
                        filter
                ))
                .flatMap(similarDocs -> {
                    String context = formatDocumentsAsContext(similarDocs);
                    String promptString = promptService.render("bugAnalysis", Map.of(
                            "draft_report", draftDescription,
                            "context_bugs", context.isEmpty() ? "Похожих багов не найдено." : context
                    ));
                    return Mono.fromFuture(llmClient.callChat(new Prompt(promptString)));
                })
                .map(this::parseLlmResponse)
                .toFuture();
    }

    private String formatDocumentsAsContext(List<Document> documents) {
        return documents.stream()
                .map(doc -> String.format("ID: %s\nСодержимое: %s",
                        doc.getMetadata().get("source"), doc.getText()))
                .collect(Collectors.joining("\n---\n"));
    }

    /**
     * Надежно парсит JSON-ответ от LLM, предварительно очищая его от
     * распространенных артефактов (Markdown, лишние пробелы).
     *
     * @param llmResponse Сырой ответ от языковой модели.
     * @return Десериализованный объект {@link BugAnalysisResponse}.
     * @throws ProcessingException если парсинг не удался даже после очистки.
     */
    private BugAnalysisResponse parseLlmResponse(String llmResponse) {
        try {
            // LLM иногда возвращает JSON внутри markdown-блока ```json ... ```. Очистим его.
            String cleanedResponse = llmResponse.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
            return objectMapper.readValue(cleanedResponse, BugAnalysisResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM даже после очистки: {}", llmResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON-ответ.", e);
        }
    }
}
