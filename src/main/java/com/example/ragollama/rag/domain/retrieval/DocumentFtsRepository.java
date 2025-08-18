package com.example.ragollama.rag.domain.retrieval;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Репозиторий для выполнения нативных SQL-запросов полнотекстового поиска (FTS).
 * Изолирует низкоуровневую логику работы с FTS от сервисного слоя.
 */
@Repository
public class DocumentFtsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Выполняет полнотекстовый поиск по ключевым словам.
     *
     * @param keywords Текст запроса.
     * @param limit    Максимальное количество возвращаемых документов.
     * @return Список найденных {@link Document}.
     */
    @SuppressWarnings("unchecked")
    public List<Document> searchByKeywords(String keywords, int limit) {
        // Преобразуем пользовательский ввод в формат, понятный to_tsquery,
        // заменяя пробелы на оператор 'И' (&). Это базовая нормализация.
        String tsQueryString = String.join(" & ", keywords.trim().split("\\s+"));

        String sql = """
            SELECT id, content, metadata, ts_rank(content_tsv, to_tsquery('russian', :query)) as rank
            FROM vector_store
            WHERE content_tsv @@ to_tsquery('russian', :query)
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
                    // Примечание: предполагается, что metadata хранится как строка JSON,
                    // и мы не будем ее здесь десериализовывать для простоты.
                    // В реальном приложении можно использовать ObjectMapper.
                    Map<String, Object> metadata = Map.of("id", id.toString());
                    return new Document(id.toString(), content, metadata);
                })
                .collect(Collectors.toList());
    }
}
