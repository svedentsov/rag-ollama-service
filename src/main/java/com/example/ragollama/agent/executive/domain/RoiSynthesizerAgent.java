package com.example.ragollama.agent.executive.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.executive.model.FinancialRoiReport;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Финальный мета-агент "AI CFO".
 * <p>
 * Синтезирует все финансовые, инженерные и продуктовые данные в единый,
 * стратегический отчет о рентабельности инвестиций (ROI).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoiSynthesizerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "roi-synthesizer-agent";
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Агрегирует все финансовые, инженерные и продуктовые метрики для расчета ROI и формирования рекомендаций.";
    }

    /** {@inheritDoc} */
    @Override
    public boolean canHandle(AgentContext context) {
        // Запускается, когда собраны все необходимые данные
        return context.payload().containsKey("cloudCosts") &&
                context.payload().containsKey("llmCosts") &&
                context.payload().containsKey("jiraEffort") &&
                context.payload().containsKey("productAnalytics");
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        log.info("RoiSynthesizerAgent: Начало синтеза финансового отчета...");

        // Собираем все отчеты от агентов-сборщиков в единый объект для передачи в промпт
        Map<String, Object> financialInputs = new HashMap<>();
        financialInputs.put("cloudCosts", context.payload().get("cloudCosts"));
        financialInputs.put("llmCosts", context.payload().get("llmCosts"));
        financialInputs.put("jiraEffort", context.payload().get("jiraEffort"));
        financialInputs.put("productAnalytics", context.payload().get("productAnalytics"));

        try {
            String inputsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(financialInputs);
            String promptString = promptService.render("roiSynthesizer", Map.of("financial_data_json", inputsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("financialRoiReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации финансовых данных для ROI-анализа.", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link FinancialRoiReport}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link FinancialRoiReport}.
     * @throws ProcessingException если парсинг не удался.
     */
    private FinancialRoiReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, FinancialRoiReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("RoiSynthesizerAgent LLM вернул невалидный JSON.", e);
        }
    }
}
