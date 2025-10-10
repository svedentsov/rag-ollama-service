package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.PrioritizedBacklog;
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
 * Мета-агент, выступающий в роли "AI Tech Lead / Product Manager".
 * <p>
 * Его задача — не выполнять атомарные действия, а принимать стратегические
 * решения. Он инкапсулирует сложную логику: сначала самостоятельно инициирует
 * сбор полной информации о состоянии проекта с помощью {@link ProjectHealthAggregatorService},
 * а затем использует LLM для анализа этих данных и формирования
 * приоритизированного бэклога в соответствии с бизнес-целями.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrioritizationAgent implements ToolAgent {

    private final ProjectHealthAggregatorService healthAggregatorService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "prioritization-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует комплексные отчеты о здоровье проекта и генерирует приоритизированный бэклог.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("goal");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String goal = (String) context.payload().get("goal");

        return Mono.fromFuture(healthAggregatorService.aggregateHealthReports(context))
                .flatMap(healthReport -> {
                    try {
                        String reportJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(healthReport);
                        String promptString = promptService.render("prioritizationAgentPrompt", Map.of(
                                "sprint_goal", goal,
                                "health_report_json", reportJson
                        ));

                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .map(this::parseLlmResponse)
                                .map(backlog -> new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        "Бэклог успешно приоритизирован. Цель: " + backlog.sprintGoal(),
                                        Map.of("prioritizedBacklog", backlog)
                                ));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ProcessingException("Ошибка сериализации отчета о здоровье.", e));
                    }
                });
    }

    private PrioritizedBacklog parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, PrioritizedBacklog.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("PrioritizationAgent LLM вернул невалидный JSON.", e);
        }
    }
}
