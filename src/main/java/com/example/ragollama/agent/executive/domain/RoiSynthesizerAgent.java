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
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Финальный мета-агент "AI CFO".
 *
 * <p>Синтезирует все финансовые, инженерные и продуктовые данные в единый,
 * стратегический отчет о рентабельности инвестиций (ROI).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoiSynthesizerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "roi-synthesizer-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Агрегирует все финансовые, инженерные и продуктовые метрики для расчета ROI и формирования рекомендаций.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("cloudCosts") &&
                context.payload().containsKey("llmCosts") &&
                context.payload().containsKey("jiraMetrics") &&
                context.payload().containsKey("productAnalytics");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        log.info("RoiSynthesizerAgent: Начало синтеза финансового отчета...");

        Map<String, Object> financialInputs = new HashMap<>();
        financialInputs.put("cloudCosts", context.payload().get("cloudCosts"));
        financialInputs.put("llmCosts", context.payload().get("llmCosts"));
        financialInputs.put("jiraEffort", context.payload().get("jiraMetrics"));
        financialInputs.put("productAnalytics", context.payload().get("productAnalytics"));

        try {
            String inputsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(financialInputs);
            String promptString = promptService.render("roiSynthesizer", Map.of("financial_data_json", inputsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .map(this::parseLlmResponse)
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("financialRoiReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации финансовых данных для ROI-анализа.", e));
        }
    }

    private FinancialRoiReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, FinancialRoiReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("RoiSynthesizerAgent LLM вернул невалидный JSON.", e);
        }
    }
}
