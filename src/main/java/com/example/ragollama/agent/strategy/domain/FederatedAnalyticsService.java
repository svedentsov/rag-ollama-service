package com.example.ragollama.agent.strategy.domain;

import com.example.ragollama.agent.config.FederationProperties;
import com.example.ragollama.agent.strategy.model.ProjectHealthSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для выполнения сложных аналитических SQL-запросов на уровне
 * всех проектов (федерации).
 */
@Service
@RequiredArgsConstructor
public class FederatedAnalyticsService {

    private final JdbcTemplate jdbcTemplate;
    private final FederationProperties federationProperties;

    /**
     * Собирает и агрегирует ключевые показатели здоровья для всех
     * проектов, перечисленных в конфигурации.
     *
     * @return Список {@link ProjectHealthSummary} для каждого проекта.
     */
    public List<ProjectHealthSummary> getProjectHealthSummaries() {
        OffsetDateTime since = OffsetDateTime.now().minusDays(30);
        String sql = """
                SELECT
                    project_id,
                    COUNT(DISTINCT test_run_id) AS total_runs,
                    COALESCE(SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END), 0) as total_failures,
                    COUNT(id) as total_executions,
                    (COALESCE(SUM(CASE WHEN status = 'PASSED' THEN 1.0 ELSE 0.0 END), 0) / COUNT(id)) * 100.0 as pass_rate
                FROM
                    test_case_run_results
                WHERE created_at >= ?
                GROUP BY project_id;
                """;

        Map<String, ProjectHealthSummary> intermediateResults = jdbcTemplate.query(sql, (ResultSet rs) -> {
            Map<String, ProjectHealthSummary> results = new java.util.HashMap<>();
            while (rs.next()) {
                String projectId = rs.getString("project_id");
                ProjectHealthSummary summary = new ProjectHealthSummary(
                        projectId,
                        "Unknown",
                        0,
                        rs.getDouble("pass_rate"),
                        5.0, // Mocked complexity
                        (int) rs.getLong("total_failures")
                );
                results.put(projectId, summary);
            }
            return results;
        }, since);

        return federationProperties.projects().stream()
                .map(proj -> {
                    ProjectHealthSummary summary = intermediateResults.getOrDefault(proj.id(),
                            new ProjectHealthSummary(proj.id(), proj.name(), 0, 0, 0, 0));
                    summary.setProjectName(proj.name());
                    summary.setOverallHealthScore(calculateHealthScore(summary));
                    return summary;
                })
                .collect(Collectors.toList());
    }

    private int calculateHealthScore(ProjectHealthSummary summary) {
        int score = (int) (summary.getTestPassRate() * 0.8);
        score -= summary.getCriticalAlertsCount() * 0.2;
        return Math.max(0, Math.min(100, score));
    }
}
