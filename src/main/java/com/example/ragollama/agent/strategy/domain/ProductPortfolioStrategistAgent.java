package com.example.ragollama.agent.strategy.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.strategy.model.PortfolioStrategyReport;
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
import java.util.stream.Collectors;

/**
 * Финальный мета-агент "AI CPO".
 * Синтезирует все данные (анализ конкурентов, фидбэк пользователей)
 * в единую продуктовую стратегию.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductPortfolioStrategistAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "product-portfolio-strategist";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Агрегирует анализ рынка и фидбэка в единую продуктовую стратегию.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        // Запускается, когда есть хотя бы один отчет о конкурентах и отчет по фидбэку
        return context.payload().containsKey("clusteredFeedback") &&
                context.payload().entrySet().stream().anyMatch(e -> e.getKey().startsWith("featureGapReport_"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        // Собираем все отчеты в единый объект для передачи в промпт
        Map<String, Object> strategicInputs = context.payload().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("featureGapReport_") || entry.getKey().equals("clusteredFeedback"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        try {
            String inputsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(strategicInputs);
            String promptString = promptService.render("productPortfolioStrategist", Map.of("strategic_inputs_json", inputsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.quarterlyGoal(),
                            Map.of("productStrategyReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации стратегических данных.", e));
        }
    }

    private PortfolioStrategyReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, PortfolioStrategyReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("ProductPortfolioStrategistAgent LLM вернул невалидный JSON.", e);
        }
    }
}
