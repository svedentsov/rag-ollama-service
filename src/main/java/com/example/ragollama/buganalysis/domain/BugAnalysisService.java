package com.example.ragollama.buganalysis.domain;

import com.example.ragollama.buganalysis.api.dto.BugAnalysisResponse;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import com.example.ragollama.shared.llm.LlmClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
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
 * Реализует полный агентский конвейер:
 * 1. Улучшает исходный запрос.
 * 2. Выполняет гибридный поиск похожих баг-репортов с использованием фильтра.
 * 3. Формирует промпт с найденным контекстом, запрашивая у LLM JSON-ответ.
 * 4. Вызывает LLM и надежно парсит полученный JSON в DTO.
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

    private static final PromptTemplate BUG_ANALYSIS_PROMPT = new PromptTemplate("""
            ТЫ — ЭКСПЕРТНЫЙ QA-АНАЛИТИК. Твоя задача — улучшить предоставленный черновик баг-репорта и проверить его на дублирование.

            ПРАВИЛА:
            1. Внимательно изучи "ЧЕРНОВИК ОТЧЕТА".
            2. Перепиши его в четкий, структурированный баг-репорт с разделами: "Шаги воспроизведения", "Ожидаемый результат", "Фактический результат".
            3. Проанализируй список "СУЩЕСТВУЮЩИЕ БАГИ". Определи, является ли новый отчет их дубликатом. Отчет считается дубликатом, если он описывает ту же самую проблему.
            4. Твой ответ должен быть ТОЛЬКО в формате JSON, без каких-либо комментариев или текста до/после.

            ЧЕРНОВИК ОТЧЕТА:
            {draft_report}

            СУЩЕСТВУЮЩИЕ БАГИ:
            {context_bugs}

            ФОРМАТ JSON ОТВЕТА:
            {
              "improvedDescription": "...",
              "isDuplicate": true,
              "duplicateCandidates": ["JIRA-123", "JIRA-456"]
            }
            """);

    /**
     * Выполняет полный цикл анализа баг-репорта.
     *
     * @param draftDescription "Сырой" текст отчета от пользователя.
     * @return {@link CompletableFuture} со структурированным результатом анализа.
     */
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
                    String promptString = BUG_ANALYSIS_PROMPT.render(Map.of(
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

    private BugAnalysisResponse parseLlmResponse(String llmResponse) {
        try {
            // LLM иногда возвращает JSON внутри markdown-блока ```json ... ```. Очистим его.
            String cleanedResponse = llmResponse.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
            return objectMapper.readValue(cleanedResponse, BugAnalysisResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM: {}", llmResponse, e);
            // Fallback: если LLM не вернул JSON, возвращаем только улучшенное описание
            return new BugAnalysisResponse(llmResponse, false, List.of());
        }
    }
}
