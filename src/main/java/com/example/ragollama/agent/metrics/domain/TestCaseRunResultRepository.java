package com.example.ragollama.agent.metrics.domain;

import com.example.ragollama.agent.metrics.model.TestCaseRunResult;
import com.example.ragollama.agent.testanalysis.model.FlakyTestInfo;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;


import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Реактивный репозиторий для TestCaseRunResult.
 */
@Repository
public interface TestCaseRunResultRepository extends ReactiveCrudRepository<TestCaseRunResult, UUID> {

    /**
     * Находит нестабильные тесты (адаптированный нативный запрос для R2DBC).
     *
     * @param since     Начальная дата.
     * @param threshold Порог.
     * @param minRuns   Минимальное количество запусков.
     * @return Поток информации о нестабильных тестах.
     */
    @Query("""
        SELECT
            t.class_name as className,
            t.test_name as testName,
            COUNT(t.id) as totalRuns,
            SUM(CASE WHEN t.status = 'FAILED' THEN 1 ELSE 0 END) as failureCount,
            (SUM(CASE WHEN t.status = 'FAILED' THEN 1.0 ELSE 0.0 END) / COUNT(t.id)) * 100.0 as flakinessRate
        FROM test_case_run_results t
        WHERE t.created_at >= :since
        GROUP BY t.class_name, t.test_name
        HAVING COUNT(t.id) >= :minRuns
           AND (SUM(CASE WHEN t.status = 'FAILED' THEN 1.0 ELSE 0.0 END) / COUNT(t.id)) > :threshold
        ORDER BY flakinessRate DESC
        """)
    Flux<FlakyTestInfo> findFlakyTests(
            @Param("since") OffsetDateTime since,
            @Param("threshold") double threshold,
            @Param("minRuns") long minRuns);
}
