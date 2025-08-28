package com.example.ragollama.qaagent.domain;

import com.example.ragollama.qaagent.model.DataProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Детерминированный сервис для сбора статистического профиля из набора данных.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataProfilerService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Выполняет SQL-запрос и строит статистический профиль для результата.
     *
     * @param sqlQuery SQL-запрос для выборки данных.
     * @return Объект {@link DataProfile}, содержащий агрегированную статистику.
     */
    public DataProfile profile(String sqlQuery) {
        log.info("Профилирование данных для SQL-запроса...");
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sqlQuery);

        if (data.isEmpty()) {
            return DataProfile.builder().rowCount(0).columnProfiles(Map.of()).build();
        }

        Map<String, DataProfile.ColumnProfile> columnProfiles = data.get(0).keySet().stream()
                .collect(Collectors.toMap(
                        columnName -> columnName,
                        columnName -> buildColumnProfile(columnName, data)
                ));

        return DataProfile.builder()
                .rowCount(data.size())
                .columnProfiles(columnProfiles)
                .build();
    }

    private DataProfile.ColumnProfile buildColumnProfile(String columnName, List<Map<String, Object>> data) {
        List<Object> values = data.stream().map(row -> row.get(columnName)).toList();
        Object firstValue = values.stream().findFirst().orElse(null);

        var profileBuilder = DataProfile.ColumnProfile.builder()
                .dataType(firstValue != null ? firstValue.getClass().getSimpleName() : "UNKNOWN")
                .nullCount(values.stream().filter(v -> v == null).count());

        if (firstValue instanceof Number) {
            DescriptiveStatistics stats = new DescriptiveStatistics();
            values.stream().filter(v -> v instanceof Number).forEach(v -> stats.addValue(((Number) v).doubleValue()));
            profileBuilder.min(stats.getMin())
                    .max(stats.getMax())
                    .mean(stats.getMean())
                    .stdDev(stats.getStandardDeviation());
        } else {
            profileBuilder.valueCounts(values.stream()
                    .filter(v -> v != null)
                    .collect(Collectors.groupingBy(Object::toString, Collectors.counting())));
        }

        return profileBuilder.build();
    }
}
