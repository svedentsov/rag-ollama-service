package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.analytics.domain.AnalyticsService;
import com.example.ragollama.agent.analytics.domain.AnalyticsService.DailyTestMetrics;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * QA-агент, отвечающий за анализ исторических данных о тестовых прогонах.
 * <p>
 * Этот агент является "аналитиком". Он извлекает "сырые" метрики из базы данных,
 * подготавливает их и использует LLM для выявления трендов, аномалий
 * и формулирования выводов на естественном языке.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestMetricsAnalyzerAgent implements ToolAgent {

    private final AnalyticsService analyticsService;
    private final LlmClient llmClient;
    private final PromptService promptService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-metrics-analyzer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует исторические метрики тестов, выявляет тренды и генерирует отчет.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("days");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        Integer days = (Integer) context.payload().get("days");
        List<DailyTestMetrics> metrics = analyticsService.getDailyTestMetrics(days);

        if (metrics.isEmpty()) {
            return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Нет данных для анализа за указанный период.", Map.of()));
        }

        String dataForLlm = metrics.stream()
                .map(m -> String.format("date: %s, failures: %d, total_tests: %d, pass_rate: %.2f%%",
                        m.date(), m.totalFailures(), m.totalTests(), m.passRate()))
                .collect(Collectors.joining("\n"));

        String promptString = promptService.render("testTrendAnalyzerPrompt", Map.of("days", days, "metricsData", dataForLlm));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .map(tuple -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Анализ метрик тестирования завершен.",
                        Map.of("analysisReport", tuple.getT1())
                ));
    }
}
