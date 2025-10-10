package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.config.FlakinessProperties;
import com.example.ragollama.agent.metrics.domain.TestCaseRunResultRepository;
import com.example.ragollama.agent.testanalysis.model.FlakinessReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * QA-агент для отслеживания "плавающих" тестов, адаптированный для R2DBC.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlakinessTrackerAgent implements ToolAgent {

    private final TestCaseRunResultRepository repository;
    private final FlakinessProperties properties;

    @Override
    public String getName() {
        return "flakiness-tracker";
    }

    @Override
    public String getDescription() {
        return "Анализирует историю прогонов и выявляет тесты с высоким процентом падений (flaky tests).";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("days");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        Integer days = (Integer) context.payload().get("days");
        double threshold = properties.failureRateThreshold() / 100.0;
        long minRuns = properties.minRunsThreshold();

        return repository.findFlakyTests(
                        OffsetDateTime.now().minusDays(days), threshold, minRuns)
                .collectList()
                .map(flakyTests -> {
                    FlakinessReport report = new FlakinessReport(
                            OffsetDateTime.now(),
                            days,
                            properties.failureRateThreshold(),
                            flakyTests
                    );

                    String summary = "Анализ нестабильности тестов завершен. Найдено кандидатов на карантин: " + flakyTests.size();
                    log.info(summary);

                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("flakinessReport", report));
                });
    }
}
