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
 * Использование `JdbcTemplate` для сложных `GROUP BY` запросов часто более
 * производительно и гибко, чем JPQL.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * DTO для хранения агрегированных метрик за один день.
     *
     * @param date          Дата.
     * @param totalRuns     Общее количество запусков сьютов.
     * @param totalTests    Общее количество выполненных тестов.
     * @param totalFailures Общее количество падений.
     * @param passRate      Процент успешно пройденных тестов.
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
     * DTO для хранения информации о медленных тестах.
     *
     * @param className         Имя класса теста.
     * @param testName          Имя метода теста.
     * @param averageDurationMs Среднее время выполнения в миллисекундах.
     */
    public record SlowTestInfo(String className, String testName, double averageDurationMs) {
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

    /**
     * Находит N самых медленных тестов по среднему времени выполнения за все время.
     *
     * @param limit Количество самых медленных тестов для возврата.
     * @return Список {@link SlowTestInfo}, отсортированный по убыванию среднего времени.
     */
    public List<SlowTestInfo> findSlowestTests(int limit) {
        String sql = """
                SELECT class_name, test_name, AVG(duration_ms) as avg_duration
                FROM test_case_run_results
                GROUP BY class_name, test_name
                HAVING AVG(duration_ms) > 0
                ORDER BY avg_duration DESC
                LIMIT ?;
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new SlowTestInfo(
                rs.getString("class_name"),
                rs.getString("test_name"),
                rs.getDouble("avg_duration")
        ), limit);
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
