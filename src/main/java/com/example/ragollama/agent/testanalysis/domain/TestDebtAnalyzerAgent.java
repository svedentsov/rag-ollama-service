package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.analytics.domain.AnalyticsService;
import com.example.ragollama.agent.testanalysis.model.*;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI-агент, который проводит комплексный анализ тестового технического долга.
 * <p>
 * Он оркестрирует сбор данных от других агентов и сервисов (для flaky-тестов,
 * медленных тестов и т.д.), агрегирует их и использует LLM для генерации
 * стратегического резюме и рекомендаций.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestDebtAnalyzerAgent implements ToolAgent {

    private final AnalyticsService analyticsService;
    private final FlakinessTrackerAgent flakinessTrackerAgent;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "test-debt-analyzer";
    }

    @Override
    public String getDescription() {
        return "Анализирует различные источники и составляет комплексный отчет о тестовом техническом долге.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        Mono<List<TestDebtItem>> flakyTestsMono = findFlakyTests(context);
        Mono<List<TestDebtItem>> slowTestsMono = findSlowTests();

        return Mono.zip(flakyTestsMono, slowTestsMono)
                .flatMap(tuple -> {
                    List<TestDebtItem> allItems = new ArrayList<>();
                    allItems.addAll(tuple.getT1());
                    allItems.addAll(tuple.getT2());

                    if (allItems.isEmpty()) {
                        return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Тестовый технический долг не обнаружен.", Map.of()));
                    }

                    try {
                        String debtJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allItems);
                        String promptString = promptService.render("testDebtSummaryPrompt", Map.of("debtItemsJson", debtJson));
                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .map(summary -> {
                                    TestDebtReport report = new TestDebtReport(summary, allItems);
                                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Отчет о тестовом техдолге успешно сгенерирован.", Map.of("testDebtReport", report));
                                });
                    } catch (JsonProcessingException e) {
                        return Mono.error(e);
                    }
                });
    }

    private Mono<List<TestDebtItem>> findFlakyTests(AgentContext context) {
        return flakinessTrackerAgent.execute(context)
                .map(agentResult -> {
                    if (!agentResult.details().containsKey("flakinessReport")) {
                        return List.of();
                    }
                    FlakinessReport report = (FlakinessReport) agentResult.details().get("flakinessReport");
                    return report.flakyTests().stream()
                            .map(ft -> new TestDebtItem(
                                    DebtType.FLAKY_TEST,
                                    Severity.HIGH,
                                    ft.className() + "#" + ft.testName(),
                                    String.format("Тест падает в %.2f%% случаев.", ft.flakinessRate()),
                                    Map.of("failureRate", ft.flakinessRate(), "totalRuns", ft.totalRuns())
                            ))
                            .toList();
                });
    }

    private Mono<List<TestDebtItem>> findSlowTests() {
        return Mono.fromCallable(() ->
                analyticsService.findSlowestTests(5).stream()
                        .map(st -> new TestDebtItem(
                                DebtType.SLOW_TEST,
                                Severity.MEDIUM,
                                st.className() + "#" + st.testName(),
                                String.format("Среднее время выполнения: %.2f мс.", st.averageDurationMs()),
                                Map.of("avgDurationMs", st.averageDurationMs())
                        ))
                        .toList()
        );
    }
}
