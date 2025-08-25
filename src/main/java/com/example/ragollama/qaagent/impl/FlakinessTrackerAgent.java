package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.config.FlakinessProperties;
import com.example.ragollama.qaagent.domain.TestCaseRunResultRepository;
import com.example.ragollama.qaagent.model.FlakinessReport;
import com.example.ragollama.qaagent.model.FlakyTestInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который отслеживает историю тестовых прогонов для выявления
 * систематически нестабильных ("плавающих") тестов.
 * <p>
 * Агент выполняет аналитический запрос к базе данных для расчета
 * процента падений (Flakiness Rate) для каждого теста и формирует
 * отчет с "кандидатами на карантин".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlakinessTrackerAgent implements QaAgent {

    private final TestCaseRunResultRepository repository;
    private final FlakinessProperties properties;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "flakiness-tracker";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует историю прогонов и выявляет тесты с высоким процентом падений (flaky tests).";
    }

    /**
     * {@inheritDoc}
     *
     * @param context Контекст, который должен содержать 'days'.
     * @return {@code true}, если все необходимые ключи присутствуют.
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("days");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Выполняет асинхронный анализ стабильности тестов.
     *
     * @param context Контекст с параметром 'days' - периодом анализа.
     * @return {@link CompletableFuture} со структурированным отчетом {@link FlakinessReport}.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            Integer days = (Integer) context.payload().get("days");
            double threshold = properties.failureRateThreshold() / 100.0;
            long minRuns = properties.minRunsThreshold();

            List<FlakyTestInfo> flakyTests = repository.findFlakyTests(
                    OffsetDateTime.now().minusDays(days), threshold, minRuns);

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
