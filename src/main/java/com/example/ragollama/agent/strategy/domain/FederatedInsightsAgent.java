package com.example.ragollama.agent.strategy.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.strategy.model.FederatedReport;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, выполняющий роль стратегического аналитика на уровне всей организации.
 * <p>
 * Агрегирует ключевые показатели качества по всем отслеживаемым проектам,
 * сравнивает их и использует LLM для генерации высокоуровневых выводов
 * и рекомендаций для инженерного руководства.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FederatedInsightsAgent implements QaAgent {

    private final FederatedAnalyticsService analyticsService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "federated-insights-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует и сравнивает метрики качества по всем проектам, генерирует стратегический отчет.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return true; // Запускается без специфического контекста
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        // Асинхронно собираем данные, затем передаем их в LLM
        return CompletableFuture.supplyAsync(analyticsService::getProjectHealthSummaries)
                .thenCompose(healthSummaries -> {
                    if (healthSummaries.isEmpty()) {
                        return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Нет данных для федеративного анализа.", Map.of()));
                    }
                    try {
                        String summariesJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(healthSummaries);
                        String promptString = promptService.render("federatedInsights", Map.of("healthDataJson", summariesJson));

                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .thenApply(summary -> {
                                    FederatedReport report = new FederatedReport(summary, healthSummaries);
                                    return new AgentResult(
                                            getName(),
                                            AgentResult.Status.SUCCESS,
                                            "Федеративный анализ успешно завершен.",
                                            Map.of("federatedReport", report)
                                    );
                                });
                    } catch (JsonProcessingException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                });
    }
}
