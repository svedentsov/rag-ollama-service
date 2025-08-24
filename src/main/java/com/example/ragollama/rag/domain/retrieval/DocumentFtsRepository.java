package com.example.ragollama.rag.domain.retrieval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Репозиторий для выполнения нативных SQL-запросов полнотекстового поиска (FTS).
 * Изолирует низкоуровневую логику работы с FTS от сервисного слоя.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class DocumentFtsRepository {

    @PersistenceContext
    private EntityManager entityManager;
    private final ObjectMapper objectMapper;

    /**
     * Выполняет полнотекстовый поиск по ключевым словам.
     *
     * @param keywords Текст запроса.
     * @param limit    Максимальное количество возвращаемых документов.
     * @return Список найденных {@link Document}.
     */
    @SuppressWarnings("unchecked")
    public List<Document> searchByKeywords(String keywords, int limit) {
        String tsQueryString = String.join(" | ", keywords.trim().split("\\s+"));
        log.debug("Выполнение FTS-поиска с запросом: {}", tsQueryString);

        String sql = """
                SELECT id, content, metadata, ts_rank(content_tsv, to_tsquery('public.russian_nostop', :query)) as rank
                FROM vector_store
                WHERE content_tsv @@ to_tsquery('public.russian_nostop', :query)
                ORDER BY rank DESC
                LIMIT :limit
                """;

        Query query = entityManager.createNativeQuery(sql, Object[].class)
                .setParameter("query", tsQueryString)
                .setParameter("limit", limit);

        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> {
                    UUID id = (UUID) row[0];
                    String content = (String) row[1];
                    Map<String, Object> metadata = parseMetadata((String) row[2]);
                    return new Document(id.toString(), content, metadata);
                })
                .collect(Collectors.toList());
    }

    /**
     * Безопасно парсит JSON-строку метаданных.
     *
     * @param metadataJson JSON-строка из БД.
     * @return {@link Map} с метаданными или пустая карта в случае ошибки.
     */
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("Не удалось распарсить метаданные из FTS-результата: {}", metadataJson, e);
            return Collections.emptyMap();
        }
    }
}
