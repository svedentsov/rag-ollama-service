package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.domain.AnalyticsService;
import com.example.ragollama.qaagent.model.*;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestDebtAnalyzerAgent implements QaAgent {

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
        return true; // Агент запускается без внешних параметров
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        // Шаг 1: Асинхронно собираем все виды техдолга
        CompletableFuture<List<TestDebtItem>> flakyTestsFuture = findFlakyTests();
        CompletableFuture<List<TestDebtItem>> slowTestsFuture = findSlowTests();

        // Шаг 2: Объединяем результаты
        return CompletableFuture.allOf(flakyTestsFuture, slowTestsFuture)
                .thenCompose(v -> {
                    List<TestDebtItem> allItems = new ArrayList<>();
                    allItems.addAll(flakyTestsFuture.join());
                    allItems.addAll(slowTestsFuture.join());

                    if (allItems.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                new AgentResult(getName(), AgentResult.Status.SUCCESS, "Тестовый технический долг не обнаружен.", Map.of())
                        );
                    }

                    // Шаг 3: Используем LLM для генерации резюме
                    try {
                        String debtJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allItems);
                        String promptString = promptService.render("testDebtSummary", Map.of("debtItemsJson", debtJson));
                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .thenApply(summary -> {
                                    TestDebtReport report = new TestDebtReport(summary, allItems);
                                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Отчет о тестовом техдолге успешно сгенерирован.", Map.of("testDebtReport", report));
                                });
                    } catch (JsonProcessingException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                });
    }

    private CompletableFuture<List<TestDebtItem>> findFlakyTests() {
        return flakinessTrackerAgent.execute(new AgentContext(Map.of("days", 30)))
                .thenApply(agentResult -> {
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

    private CompletableFuture<List<TestDebtItem>> findSlowTests() {
        return CompletableFuture.supplyAsync(() ->
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
