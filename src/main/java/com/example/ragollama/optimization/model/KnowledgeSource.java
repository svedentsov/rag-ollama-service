package com.example.ragollama.optimization.model;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Определяет контракт для "источника правды".
 * Каждая реализация отвечает за поиск доказательств для заданного утверждения
 * в своем источнике данных (векторная БД, код, API и т.д.).
 */
public interface KnowledgeSource {
    /**
     * Возвращает уникальное имя источника.
     *
     * @return Имя источника (e.g., "VectorStore", "Codebase").
     */
    String getSourceName();

    /**
     * Асинхронно ищет доказательства, подтверждающие или опровергающие утверждение.
     *
     * @param claim Утверждение для проверки (например, "Таймаут для Ollama равен 30 секундам").
     * @return {@link Mono} со списком найденных фрагментов текста (доказательств).
     */
    Mono<List<String>> findEvidence(String claim);
}
