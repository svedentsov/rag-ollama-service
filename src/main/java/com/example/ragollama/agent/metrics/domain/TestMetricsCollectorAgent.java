package com.example.ragollama.agent.metrics.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.metrics.model.TestCaseRunResult;
import com.example.ragollama.agent.metrics.model.TestResult;
import com.example.ragollama.agent.metrics.model.TestRunMetric;
import com.example.ragollama.agent.metrics.tools.JUnitXmlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * QA-агент, отвечающий за сбор "сырых" метрик из результатов тестовых прогонов.
 * <p>
 * Этот агент является "сборщиком". Его задача — принять JUnit XML-отчет,
 * распарсить его, и сохранить в базу данных как агрегированную статистику
 * по всему прогону (в {@code test_run_metrics}), так и гранулярные результаты
 * по каждому отдельному тест-кейсу (в {@code test_case_run_results}).
 * Он также обеспечивает привязку данных к конкретному проекту для
 * поддержки федеративного анализа.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestMetricsCollectorAgent implements ToolAgent {

    private final JUnitXmlParser jUnitXmlParser;
    private final TestMetricsRepository testMetricsRepository;
    private final TestCaseRunResultRepository testCaseRunResultRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-metrics-collector";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Парсит JUnit XML отчет и сохраняет агрегированные и гранулярные метрики в базу данных.";
    }

    /**
     * {@inheritDoc}
     *
     * @param context Контекст, который должен содержать 'projectId', 'junitXmlContent' и 'commitHash'.
     * @return {@code true}, если все необходимые ключи присутствуют.
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("projectId") &&
                context.payload().containsKey("junitXmlContent") &&
                context.payload().containsKey("commitHash");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Выполняет парсинг и сохранение в одной транзакции. Если сохранение
     * гранулярных результатов не удастся, сохранение агрегированной метрики
     * также будет отменено, обеспечивая консистентность данных.
     *
     * @param context Контекст с данными о тестовом прогоне.
     * @return {@link CompletableFuture} с результатом операции.
     */
    @Override
    @Transactional
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        // Выполняем в CompletableFuture, чтобы соответствовать интерфейсу,
        // но сама логика может быть синхронной, т.к. она быстрая.
        return CompletableFuture.supplyAsync(() -> {
            String projectId = (String) context.payload().get("projectId");
            String xmlContent = (String) context.payload().get("junitXmlContent");
            List<TestResult> testResults = jUnitXmlParser.parse(xmlContent);

            long passed = testResults.stream().filter(r -> r.status() == TestResult.Status.PASSED).count();
            long failed = testResults.stream().filter(r -> r.status() == TestResult.Status.FAILED).count();
            long skipped = testResults.stream().filter(r -> r.status() == TestResult.Status.SKIPPED).count();
            long totalDuration = testResults.stream().mapToLong(TestResult::durationMs).sum();

            // Шаг 1: Сохраняем агрегированную запись о прогоне
            TestRunMetric metric = TestRunMetric.builder()
                    .projectId(projectId)
                    .commitHash((String) context.payload().get("commitHash"))
                    .branchName((String) context.payload().get("branchName"))
                    .totalCount(testResults.size())
                    .passedCount((int) passed)
                    .failedCount((int) failed)
                    .skippedCount((int) skipped)
                    .durationMs(totalDuration)
                    .runTimestamp(OffsetDateTime.now())
                    .build();
            testMetricsRepository.save(metric);

            // Шаг 2: Сохраняем гранулярные результаты для каждого тест-кейса
            List<TestCaseRunResult> caseResults = testResults.stream()
                    .map(tr -> TestCaseRunResult.builder()
                            .projectId(projectId)
                            .testRun(metric)
                            .className(tr.className())
                            .testName(tr.testName())
                            .status(tr.status())
                            .failureDetails(tr.failureDetails())
                            .durationMs(tr.durationMs())
                            .createdAt(OffsetDateTime.now()) // Явно устанавливаем, чтобы было консистентно
                            .build())
                    .collect(Collectors.toList());
            testCaseRunResultRepository.saveAll(caseResults);

            String summary = String.format("Сохранены метрики для %d тестов проекта '%s'.", testResults.size(), projectId);
            log.info(summary);
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("metricId", metric.getId()));
        });
    }
}
