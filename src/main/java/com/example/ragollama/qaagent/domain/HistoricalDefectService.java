package com.example.ragollama.qaagent.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Сервис для извлечения исторических данных о дефектах из БД.
 */
@Service
@RequiredArgsConstructor
public class HistoricalDefectService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Рассчитывает количество падений для каждого файла за период.
     *
     * @param days Период анализа.
     * @return Карта, где ключ - имя класса, значение - количество падений.
     */
    public Map<String, Long> getFailureCountsByClass(int days) {
        OffsetDateTime since = OffsetDateTime.now().minusDays(days);
        String sql = """
                SELECT class_name, COUNT(*) as failure_count
                FROM test_case_run_results
                WHERE status = 'FAILED' AND created_at >= ?
                GROUP BY class_name;
                """;

        return jdbcTemplate.query(sql, (rs) -> {
            Map<String, Long> results = new java.util.HashMap<>();
            while (rs.next()) {
                // Преобразуем имя класса в путь к файлу
                String filePath = "src/main/java/" + rs.getString("class_name").replace('.', '/') + ".java";
                results.put(filePath, rs.getLong("failure_count"));
            }
            return results;
        }, since);
    }
}
