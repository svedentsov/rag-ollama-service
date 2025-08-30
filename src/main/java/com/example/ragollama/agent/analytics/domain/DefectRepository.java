package com.example.ragollama.agent.analytics.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Репозиторий для извлечения документов, связанных с дефектами.
 */
@Repository
@RequiredArgsConstructor
public class DefectRepository {

    private final VectorStore vectorStore;

    /**
     * Находит все документы, помеченные как 'bug_report', за указанный период.
     *
     * @param days Количество последних дней для поиска.
     * @return Список {@link Document}.
     */
    public List<Document> findRecentDefects(int days) {
        OffsetDateTime since = OffsetDateTime.now().minusDays(days);
        // В pgvector мы не можем фильтровать по дате напрямую, так как это неиндексированное
        // поле внутри JSONB. Этот фильтр является примером, и в production
        // для этого потребовалась бы более сложная схема или индексация.
        // Пока что мы извлечем все баги и отфильтруем их в приложении.

        // Создаем фильтр для поиска только документов с метаданными doc_type = 'bug_report'
        Filter.Expression filter = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("metadata.doc_type"),
                new Filter.Value("bug_report")
        );

        // Используем "запрос-заглушку" для извлечения документов по фильтру.
        // В реальной системе может потребоваться более сложная логика.
        SearchRequest request = SearchRequest.builder()
                .query("*") // Некоторые VectorStore могут не поддерживать `*`
                .filterExpression(filter)
                .topK(1000) // Ограничиваем максимальное количество для анализа
                .build();

        // TODO: Добавить фильтрацию по дате в приложении, если VectorStore API не поддерживает это.
        return vectorStore.similaritySearch(request);
    }
}
