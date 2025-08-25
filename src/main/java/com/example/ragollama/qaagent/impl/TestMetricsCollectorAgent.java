package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.domain.TestMetricsRepository;
import com.example.ragollama.qaagent.model.TestResult;
import com.example.ragollama.qaagent.model.TestRunMetric;
import com.example.ragollama.qaagent.tools.JUnitXmlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, отвечающий за сбор "сырых" метрик из результатов тестовых прогонов.
 * <p>
 * Этот агент является "сборщиком". Его единственная задача — принять JUnit XML-отчет,
 * распарсить его, агрегировать базовые показатели и сохранить их в базу данных
 * для последующего анализа.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestMetricsCollectorAgent implements QaAgent {

    private final JUnitXmlParser jUnitXmlParser;
    private final TestMetricsRepository testMetricsRepository;

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
        return "Парсит JUnit XML отчет и сохраняет агрегированные метрики в базу данных.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("junitXmlContent") && context.payload().containsKey("commitHash");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            String xmlContent = (String) context.payload().get("junitXmlContent");
            List<TestResult> testResults = jUnitXmlParser.parse(xmlContent);

            long passed = testResults.stream().filter(r -> r.status() == TestResult.Status.PASSED).count();
            long failed = testResults.stream().filter(r -> r.status() == TestResult.Status.FAILED).count();
            long skipped = testResults.stream().filter(r -> r.status() == TestResult.Status.SKIPPED).count();

            TestRunMetric metric = TestRunMetric.builder()
                    .commitHash((String) context.payload().get("commitHash"))
                    .branchName((String) context.payload().get("branchName"))
                    .totalCount(testResults.size())
                    .passedCount((int) passed)
                    .failedCount((int) failed)
                    .skippedCount((int) skipped)
                    .durationMs(0L) // Для простоты, парсинг duration требует доработки JUnitXmlParser
                    .runTimestamp(OffsetDateTime.now())
                    .build();

            testMetricsRepository.save(metric);
            String summary = String.format("Сохранены метрики для %d тестов.", testResults.size());
            log.info(summary);
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("metricId", metric.getId()));
        });
    }
}
