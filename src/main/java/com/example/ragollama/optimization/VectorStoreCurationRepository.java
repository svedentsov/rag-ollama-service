package com.example.ragollama.optimization;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Репозиторий для выполнения кастомных, низкоуровневых операций
 * по обслуживанию и курированию данных в `vector_store`.
 */
@Repository
@RequiredArgsConstructor
public class VectorStoreCurationRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Находит ID документов, которые нуждаются в курировании.
     * Критерий: в метаданных отсутствует поле 'last_curated_at'.
     *
     * @param limit Максимальное количество ID для возврата.
     * @return Список UUID документов.
     */
    public List<UUID> findDocumentsForCuration(int limit) {
        final String sql = "SELECT DISTINCT CAST(metadata ->> 'documentId' AS UUID) " +
                "FROM vector_store " +
                "WHERE metadata ->> 'last_curated_at' IS NULL " +
                "LIMIT ?";
        return jdbcTemplate.queryForList(sql, UUID.class, limit);
    }

    /**
     * Собирает полный текст документа по его ID, конкатенируя
     * содержимое всех его чанков.
     *
     * @param documentId ID документа.
     * @return Полный текст документа.
     */
    public String getFullTextByDocumentId(UUID documentId) {
        final String sql = "SELECT content FROM vector_store WHERE metadata ->> 'documentId' = ? ORDER BY id";
        List<String> contents = jdbcTemplate.queryForList(sql, String.class, documentId.toString());
        return String.join("\n\n", contents);
    }

    /**
     * Атомарно обновляет метаданные для всех чанков, принадлежащих
     * одному документу, добавляя в них новые поля.
     *
     * @param documentId     ID документа, чьи чанки нужно обновить.
     * @param fieldsToUpdate Карта с новыми полями и их значениями для добавления в JSONB.
     * @return Количество обновленных чанков.
     */
    public int updateMetadataByDocumentId(UUID documentId, Map<String, Object> fieldsToUpdate) {
        // `jsonb_set` позволяет атомарно обновить JSONB поле
        final String sql = "UPDATE vector_store " +
                "SET metadata = jsonb_set(metadata, ?, ?, true) " +
                "WHERE metadata ->> 'documentId' = ?";

        int totalUpdated = 0;
        for (Map.Entry<String, Object> entry : fieldsToUpdate.entrySet()) {
            String jsonPath = "{" + entry.getKey() + "}";
            String jsonValue = convertValueToJsonbString(entry.getValue());
            totalUpdated += jdbcTemplate.update(sql, jsonPath, jsonValue, documentId.toString());
        }
        return totalUpdated;
    }

    private String convertValueToJsonbString(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        if (value instanceof List) {
            // Простая реализация для списка строк
            return ((List<?>) value).stream()
                    .map(item -> "\"" + item.toString() + "\"")
                    .collect(Collectors.joining(", ", "[", "]"));
        }
        return value.toString();
    }
}
