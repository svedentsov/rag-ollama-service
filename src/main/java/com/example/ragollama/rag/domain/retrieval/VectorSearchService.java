package com.example.ragollama.rag.domain.retrieval;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.List;

/**
 * Определяет контракт для сервиса, выполняющего поиск в векторном хранилище.
 * <p>
 * Этот интерфейс является ключевым элементом для реализации паттерна "Декоратор",
 * позволяя отделить основную логику поиска от сквозных задач, таких как кэширование.
 */
public interface VectorSearchService {
    /**
     * Выполняет поиск по схожести в векторном хранилище.
     *
     * @param request Параметры поиска.
     * @return Список найденных документов.
     */
    List<Document> search(SearchRequest request);
}
