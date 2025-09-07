package com.example.ragollama.agent.datageneration.domain;

import com.example.ragollama.shared.processing.PiiRedactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Доменный сервис, отвечающий за выполнение SQL-запросов и маскирование данных.
 * <p>
 * Этот сервис инкапсулирует потенциально опасные операции (выполнение SQL)
 * и логику безопасности (маскирование PII), предоставляя агенту чистый и безопасный API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSubsetService {

    private final JdbcTemplate jdbcTemplate;
    private final PiiRedactionService piiRedactionService;
    private final ObjectMapper objectMapper;

    /**
     * Выполняет сгенерированный SQL-запрос и маскирует результаты.
     *
     * @param sqlQuery SQL-запрос для выполнения.
     * @return Список карт, представляющих строки с замаскированными данными.
     */
    public List<Map<String, Object>> executeAndMask(String sqlQuery) {
        log.warn("Выполнение потенциально небезопасного, сгенерированного AI SQL-запроса: {}", sqlQuery);
        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(sqlQuery);

        return rawData.stream()
                .map(this::maskRow)
                .collect(Collectors.toList());
    }

    /**
     * Маскирует чувствительные данные в одной строке (представленной как Map).
     *
     * @param row Карта, представляющая одну строку из ResultSet.
     * @return Та же карта, но со значениями, прошедшими через PiiRedactionService.
     */
    private Map<String, Object> maskRow(Map<String, Object> row) {
        return row.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> maskValue(entry.getValue())
                ));
    }

    /**
     * Применяет маскирование к одному значению, если оно является строкой.
     */
    private Object maskValue(Object value) {
        if (value instanceof String stringValue) {
            // Для безопасности, также маскируем JSON-строки внутри ячеек
            try {
                String jsonString = objectMapper.writeValueAsString(stringValue);
                return piiRedactionService.redact(jsonString);
            } catch (JsonProcessingException e) {
                // Если это не JSON, маскируем как обычный текст
                return piiRedactionService.redact(stringValue);
            }
        }
        return value;
    }
}
