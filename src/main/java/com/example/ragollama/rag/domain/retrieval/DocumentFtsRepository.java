package com.example.ragollama.rag.domain.retrieval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Репозиторий для выполнения нативных SQL-запросов полнотекстового поиска (FTS) с использованием R2DBC.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class DocumentFtsRepository {

    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;

    /**
     * Выполняет полнотекстовый поиск по ключевым словам.
     *
     * @param keywords Текст запроса.
     * @param limit    Максимальное количество возвращаемых документов.
     * @return {@link Mono} со списком найденных {@link Document}.
     */
    public Mono<List<Document>> searchByKeywords(String keywords, int limit) {
        String tsQueryString = String.join(" | ", keywords.trim().split("\\s+"));
        log.debug("Выполнение FTS-поиска с запросом: {}", tsQueryString);

        String sql = """
                SELECT id, content, metadata, ts_rank(content_tsv, to_tsquery('public.russian_nostop', :query)) as rank
                FROM vector_store
                WHERE content_tsv @@ to_tsquery('public.russian_nostop', :query)
                ORDER BY rank DESC
                LIMIT :limit
                """;

        return databaseClient.sql(sql)
                .bind("query", tsQueryString)
                .bind("limit", limit)
                .map((row, metadata) -> {
                    UUID id = row.get("id", UUID.class);
                    String content = row.get("content", String.class);
                    String metaJson = row.get("metadata", String.class);
                    Map<String, Object> meta = parseMetadata(metaJson);
                    return new Document(id.toString(), content, meta);
                })
                .all()
                .collectList();
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Не удалось распарсить метаданные из FTS-результата: {}", metadataJson, e);
            return Collections.emptyMap();
        }
    }
}
