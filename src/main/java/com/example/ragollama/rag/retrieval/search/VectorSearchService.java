package com.example.ragollama.rag.retrieval.search;

import jakarta.annotation.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Определяет контракт для сервиса, выполняющего поиск в векторном хранилище.
 */
public interface VectorSearchService {
    /**
     * Выполняет поиск по схожести для нескольких запросов.
     *
     * @param queries             Список текстов запросов.
     * @param topK                Количество извлекаемых документов.
     * @param similarityThreshold Порог схожести.
     * @param filter              Опциональный фильтр метаданных.
     * @param efSearch            Опциональное значение hnsw.ef_search.
     * @return Mono со списком уникальных документов.
     */
    Mono<List<Document>> search(List<String> queries, int topK, double similarityThreshold, @Nullable Filter.Expression filter, @Nullable Integer efSearch);
}