package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.metrics.model.TestResult;
import com.example.ragollama.agent.metrics.tools.JUnitXmlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * QA-агент, который обнаруживает "плавающие" (flaky) тесты путем сравнения
 * результатов двух тестовых прогонов.
 * <p>
 * Основная эвристика: тест считается "плавающим", если он упал в текущем
 * прогоне, но успешно прошел в эталонном (baseline) прогоне.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestFlakyDetectorAgent implements ToolAgent {

    private final JUnitXmlParser jUnitXmlParser;

    @Override
    public String getName() {
        return "flaky-test-detector";
    }

    @Override
    public String getDescription() {
        return "Обнаруживает 'плавающие' тесты, сравнивая текущий и эталонный отчеты о тестировании.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("currentTestReportContent") &&
                context.payload().containsKey("baselineTestReportContent");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        // Парсинг может быть долгим, поэтому выполняем его асинхронно
        return CompletableFuture.supplyAsync(() -> {
            String currentReport = (String) context.payload().get("currentTestReportContent");
            String baselineReport = (String) context.payload().get("baselineTestReportContent");

            List<TestResult> currentResults = jUnitXmlParser.parse(currentReport);
            List<TestResult> baselineResults = jUnitXmlParser.parse(baselineReport);

            // Находим тесты, которые упали в текущем прогоне
            List<TestResult> currentFailures = currentResults.stream()
                    .filter(r -> r.status() == TestResult.Status.FAILED)
                    .toList();

            // Создаем множество успешно пройденных тестов в эталонном прогоне для быстрого поиска
            Set<String> baselinePassedTests = baselineResults.stream()
                    .filter(r -> r.status() == TestResult.Status.PASSED)
                    .map(TestResult::getFullName)
                    .collect(Collectors.toSet());

            // "Плавающие" тесты - это те, что упали сейчас, но проходили в эталоне
            List<String> flakyTests = currentFailures.stream()
                    .filter(failure -> baselinePassedTests.contains(failure.getFullName()))
                    .map(TestResult::getFullName)
                    .toList();

            String summary;
            if (flakyTests.isEmpty()) {
                summary = "Анализ завершен. Потенциально 'плавающих' тестов не обнаружено.";
                log.info(summary);
            } else {
                summary = String.format("Внимание! Обнаружено %d потенциально 'плавающих' тестов.", flakyTests.size());
                log.warn("{}. Список: {}", summary, flakyTests);
            }

            return new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    summary,
                    Map.of("flakyTests", flakyTests)
            );
        });
    }
}
