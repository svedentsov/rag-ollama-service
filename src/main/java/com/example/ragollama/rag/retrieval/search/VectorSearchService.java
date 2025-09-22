package com.example.ragollama.rag.retrieval.search;

import jakarta.annotation.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

/**
 * Определяет контракт для сервиса, выполняющего поиск в векторном хранилище.
 */
public interface VectorSearchService {
    /**
     * Выполняет поиск по схожести в векторном хранилище для нескольких запросов.
     *
     * @param queries             Список текстов запросов.
     * @param topK                Количество извлекаемых документов для каждого запроса.
     * @param similarityThreshold Порог схожести.
     * @param filter              Опциональный фильтр метаданных.
     * @param efSearch            (Новое) Опциональное значение hnsw.ef_search для текущего запроса.
     * @return Список уникальных, найденных документов.
     */
    List<Document> search(List<String> queries, int topK, double similarityThreshold, @Nullable Filter.Expression filter, @Nullable Integer efSearch);
}
