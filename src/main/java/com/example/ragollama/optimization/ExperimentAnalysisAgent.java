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
import reactor.core.publisher.Mono;

import java.util.Map;

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
    private final JsonExtractorUtil jsonExtractorUtil;

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
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        Object resultsObject = context.payload().get("experimentResults");

        if (!(resultsObject instanceof Map)) {
            String errorMessage = "Ошибка контракта: ExperimentAnalysisAgent ожидал Map в 'experimentResults', но получил " +
                    (resultsObject == null ? "null" : resultsObject.getClass().getName());
            log.error(errorMessage);
            return Mono.error(new ProcessingException(errorMessage));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> results = (Map<String, Object>) resultsObject;

        try {
            String resultsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
            String promptString = promptService.render("experimentAnalyzerPrompt", Map.of("results_json", resultsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .map(this::parseLlmResponse)
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("experimentReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации результатов эксперимента.", e));
        }
    }

    private ExperimentReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ExperimentReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от ExperimentAnalysisAgent: {}", jsonResponse, e);
            throw new ProcessingException("ExperimentAnalysisAgent LLM вернул невалидный JSON.", e);
        }
    }
}
