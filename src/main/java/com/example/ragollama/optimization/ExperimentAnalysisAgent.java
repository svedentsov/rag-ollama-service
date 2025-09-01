package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.ExperimentReport;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который анализирует результаты A/B-тестирования,
 * определяет конфигурацию-победителя и генерирует отчет.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExperimentAnalysisAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "experiment-analysis-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует результаты A/B-теста и объявляет победителя.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("experimentResults");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Выполняет безопасное извлечение данных из контекста, проверяя их тип,
     * а затем передает их в LLM для финального анализа и вынесения вердикта.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        Object resultsObject = context.payload().get("experimentResults");

        // Безопасное приведение типа для устранения unchecked cast warning
        if (!(resultsObject instanceof Map)) {
            String errorMessage = "Ошибка контракта: ExperimentAnalysisAgent ожидал Map в 'experimentResults', но получил " +
                    (resultsObject == null ? "null" : resultsObject.getClass().getName());
            log.error(errorMessage);
            return CompletableFuture.failedFuture(new ProcessingException(errorMessage));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> results = (Map<String, Object>) resultsObject;

        try {
            String resultsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
            String promptString = promptService.render("experimentAnalyzer", Map.of("results_json", resultsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("experimentReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации результатов эксперимента.", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link ExperimentReport}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link ExperimentReport}.
     * @throws ProcessingException если парсинг не удался.
     */
    private ExperimentReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ExperimentReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от ExperimentAnalysisAgent: {}", jsonResponse, e);
            throw new ProcessingException("ExperimentAnalysisAgent LLM вернул невалидный JSON.", e);
        }
    }
}
