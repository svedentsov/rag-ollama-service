package com.example.ragollama.optimization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Репозиторий для выполнения кастомных, низкоуровневых операций
 * по обслуживанию и курированию данных в `vector_store` с использованием R2DBC.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VectorStoreCurationRepository {

    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;

    /**
     * Находит ID документов, которые нуждаются в курировании.
     *
     * @param limit Максимальное количество ID для возврата.
     * @return Поток UUID документов.
     */
    public Flux<UUID> findDocumentsForCuration(int limit) {
        final String sql = "SELECT DISTINCT CAST(metadata ->> 'documentId' AS UUID) " +
                "FROM vector_store " +
                "WHERE metadata ->> 'last_curated_at' IS NULL " +
                "LIMIT :limit";
        return databaseClient.sql(sql)
                .bind("limit", limit)
                .map((row, metadata) -> row.get(0, UUID.class))
                .all();
    }

    /**
     * Собирает полный текст документа по его ID.
     *
     * @param documentId ID документа.
     * @return Mono с полным текстом документа.
     */
    public Mono<String> getFullTextByDocumentId(UUID documentId) {
        final String sql = "SELECT content FROM vector_store WHERE metadata ->> 'documentId' = :docId ORDER BY id";
        return databaseClient.sql(sql)
                .bind("docId", documentId.toString())
                .map((row, metadata) -> row.get("content", String.class))
                .all()
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Атомарно обновляет метаданные для всех чанков одного документа.
     *
     * @param documentId     ID документа.
     * @param fieldsToUpdate Карта с новыми полями.
     * @return Mono с количеством обновленных чанков.
     */
    public Mono<Long> updateMetadataByDocumentId(UUID documentId, Map<String, Object> fieldsToUpdate) {
        if (fieldsToUpdate.isEmpty()) {
            return Mono.just(0L);
        }
        // Создаем JSONB-объект для слияния
        String mergeObject;
        try {
            mergeObject = objectMapper.writeValueAsString(fieldsToUpdate);
        } catch (JsonProcessingException e) {
            return Mono.error(new IllegalArgumentException("Не удалось сериализовать поля для обновления.", e));
        }

        final String sql = "UPDATE vector_store " +
                "SET metadata = metadata || :mergeObject::jsonb " +
                "WHERE metadata ->> 'documentId' = :docId";

        return databaseClient.sql(sql)
                .bind("mergeObject", mergeObject)
                .bind("docId", documentId.toString())
                .fetch()
                .rowsUpdated();
    }
}
