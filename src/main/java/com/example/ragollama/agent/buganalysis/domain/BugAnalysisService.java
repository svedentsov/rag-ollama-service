package com.example.ragollama.agent.buganalysis.domain;

import com.example.ragollama.agent.buganalysis.api.dto.BugAnalysisResponse;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
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
import java.util.stream.Collectors;

/**
 * Сервис-оркестратор для агента анализа баг-репортов.
 * <p>
 * Эта версия использует полностью реактивный подход на базе Project Reactor,
 * возвращая {@link Mono} для построения неблокирующего конвейера обработки.
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

    /**
     * Асинхронно анализирует баг-репорт, используя неблокирующий RAG-конвейер.
     *
     * @param draftDescription Черновик описания бага от пользователя.
     * @return {@link Mono}, который по завершении будет содержать
     * структурированный {@link BugAnalysisResponse}.
     */
    public Mono<BugAnalysisResponse> analyzeBugReport(String draftDescription) {
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
                    return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED));
                })
                .map(this::parseLlmResponse);
    }

    /**
     * Форматирует список документов в единую строку для передачи в промпт LLM.
     *
     * @param documents Список найденных документов.
     * @return Строка с контекстом.
     */
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
            String cleanedResponse = llmResponse.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
            return objectMapper.readValue(cleanedResponse, BugAnalysisResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM даже после очистки: {}", llmResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON-ответ.", e);
        }
    }
}
