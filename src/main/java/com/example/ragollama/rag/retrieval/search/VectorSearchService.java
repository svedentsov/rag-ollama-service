package com.example.ragollama.rag.retrieval.search;

import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

/**
 * Определяет контракт для сервиса, выполняющего асинхронный поиск в векторном хранилище.
 * <p>
 * Эта версия интерфейса была изменена на синхронную (возвращает {@code List<Document>}),
 * чтобы обеспечить корректную работу с декларативным кэшированием Spring (`@Cacheable`).
 * Асинхронность теперь обеспечивается на более высоком уровне, в вызывающем сервисе
 * {@link HybridRetrievalStrategy}.
 */
public interface VectorSearchService {
    /**
     * Выполняет поиск по схожести в векторном хранилище для нескольких запросов.
     *
     * @param queries             Список текстов запросов.
     * @param topK                Количество извлекаемых документов для каждого запроса.
     * @param similarityThreshold Порог схожести.
     * @param filter              Опциональный фильтр метаданных.
     * @return Список уникальных, найденных документов.
     */
    List<Document> search(List<String> queries, int topK, double similarityThreshold, Filter.Expression filter);
}
