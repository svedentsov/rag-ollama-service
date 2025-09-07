package com.example.ragollama.rag.retrieval.search;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Определяет контракт для сервиса, выполняющего асинхронный поиск в векторном хранилище.
 * <p>
 * Этот интерфейс является ключевым элементом для реализации паттерна "Декоратор",
 * позволяя отделить основную логику поиска от сквозных задач, таких как кэширование.
 */
public interface VectorSearchService {
    /**
     * Асинхронно выполняет поиск по схожести в векторном хранилище для нескольких запросов.
     *
     * @param queries             Список текстов запросов.
     * @param topK                Количество извлекаемых документов для каждого запроса.
     * @param similarityThreshold Порог схожести.
     * @param filter              Опциональный фильтр метаданных.
     * @return {@link Mono}, который по завершении будет содержать объединенный и
     * дедуплицированный список найденных документов.
     */
    Mono<List<Document>> search(List<String> queries, int topK, double similarityThreshold, Filter.Expression filter);
}
