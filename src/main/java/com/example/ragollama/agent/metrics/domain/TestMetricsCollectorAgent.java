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
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * QA-агент для сбора метрик, адаптированный для R2DBC.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestMetricsCollectorAgent implements ToolAgent {

    private final JUnitXmlParser jUnitXmlParser;
    private final TestMetricsRepository testMetricsRepository;
    private final TestCaseRunResultRepository testCaseRunResultRepository;

    @Override
    public String getName() {
        return "test-metrics-collector";
    }

    @Override
    public String getDescription() {
        return "Парсит JUnit XML и сохраняет метрики в базу данных.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("projectId") &&
                context.payload().containsKey("junitXmlContent") &&
                context.payload().containsKey("commitHash");
    }

    @Override
    @Transactional
    public Mono<AgentResult> execute(AgentContext context) {
        String projectId = (String) context.payload().get("projectId");
        String xmlContent = (String) context.payload().get("junitXmlContent");
        List<TestResult> testResults = jUnitXmlParser.parse(xmlContent);

        long passed = testResults.stream().filter(r -> r.status() == TestResult.Status.PASSED).count();
        long failed = testResults.stream().filter(r -> r.status() == TestResult.Status.FAILED).count();
        long skipped = testResults.stream().filter(r -> r.status() == TestResult.Status.SKIPPED).count();
        long totalDuration = testResults.stream().mapToLong(TestResult::durationMs).sum();

        TestRunMetric metric = TestRunMetric.builder()
                .id(UUID.randomUUID())
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

        return testMetricsRepository.save(metric)
                .flatMap(savedMetric -> {
                    List<TestCaseRunResult> caseResults = testResults.stream()
                            .map(tr -> TestCaseRunResult.builder()
                                    .projectId(projectId)
                                    .testRunId(savedMetric.getId())
                                    .className(tr.className())
                                    .testName(tr.testName())
                                    .status(tr.status())
                                    .failureDetails(tr.failureDetails())
                                    .durationMs(tr.durationMs())
                                    .createdAt(OffsetDateTime.now())
                                    .build())
                            .toList();
                    return testCaseRunResultRepository.saveAll(caseResults).collectList();
                })
                .map(savedCases -> {
                    String summary = String.format("Сохранены метрики для %d тестов проекта '%s'.", testResults.size(), projectId);
                    log.info(summary);
                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("metricId", metric.getId()));
                });
    }
}
