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
import reactor.core.publisher.Mono;

import java.util.Map;
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
    private final JsonExtractorUtil jsonExtractorUtil;

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
        return context.payload().containsKey("clusteredFeedback") &&
                context.payload().entrySet().stream().anyMatch(e -> e.getKey().startsWith("featureGapReport_"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        Map<String, Object> strategicInputs = context.payload().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("featureGapReport_") || entry.getKey().equals("clusteredFeedback"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        try {
            String inputsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(strategicInputs);
            String promptString = promptService.render("productPortfolioStrategist", Map.of("strategic_inputs_json", inputsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                    .map(tuple -> parseLlmResponse(tuple.getT1()))
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.quarterlyGoal(),
                            Map.of("productStrategyReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации стратегических данных.", e));
        }
    }

    private PortfolioStrategyReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, PortfolioStrategyReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("ProductPortfolioStrategistAgent LLM вернул невалидный JSON.", e);
        }
    }
}
