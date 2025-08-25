package com.example.ragollama.qaagent.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Сервис для выполнения сложных аналитических SQL-запросов и агрегации данных.
 * <p>
 * Этот сервис является "Data Layer" для аналитических агентов, предоставляя им
 * готовые, пред-агрегированные данные для последующей интерпретации с помощью LLM.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * DTO для хранения агрегированных метрик за один день.
     */
    public record DailyTestMetrics(
            LocalDate date,
            long totalRuns,
            long totalTests,
            long totalFailures,
            double passRate
    ) {
    }

    /**
     * Извлекает и агрегирует ежедневные метрики тестовых прогонов за указанный период.
     *
     * @param days Количество последних дней для анализа.
     * @return Список {@link DailyTestMetrics}, отсортированный по дате.
     */
    public List<DailyTestMetrics> getDailyTestMetrics(int days) {
        OffsetDateTime since = OffsetDateTime.now().minusDays(days);
        String sql = """
                SELECT
                    run_timestamp::date AS run_date,
                    COUNT(*) AS total_runs,
                    SUM(total_count) AS total_tests,
                    SUM(failed_count) AS total_failures,
                    (SUM(passed_count)::decimal / SUM(total_count)::decimal) * 100 AS pass_rate
                FROM
                    test_run_metrics
                WHERE
                    run_timestamp >= ?
                GROUP BY
                    run_date
                ORDER BY
                    run_date ASC;
                """;

        return jdbcTemplate.query(sql, this::mapRowToDailyTestMetrics, since);
    }

    private DailyTestMetrics mapRowToDailyTestMetrics(ResultSet rs, int rowNum) throws SQLException {
        return new DailyTestMetrics(
                rs.getDate("run_date").toLocalDate(),
                rs.getLong("total_runs"),
                rs.getLong("total_tests"),
                rs.getLong("total_failures"),
                rs.getDouble("pass_rate")
        );
    }
}
