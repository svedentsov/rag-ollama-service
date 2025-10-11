package com.example.ragollama.agent.routing;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Перечисление, определяющее возможные намерения (intent) пользователя.
 * <p>
 * В эту версию добавлена аннотация {@code @JsonAlias}, чтобы сделать
 * десериализацию более устойчивой к незначительным вариациям в ответе LLM
 * (например, "CHIT CHAT" вместо "CHITCHAT" или кириллические аналоги).
 */
public enum QueryIntent {

    /**
     * Вопрос, для ответа на который требуется извлечение информации (RAG).
     */
    RAG_QUERY,

    /**
     * Запрос на анализ описания бага.
     */
    @JsonAlias({"BUG ANALYSIS", "БОГ_АНАЛИЗА"})
    BUG_ANALYSIS,

    /**
     * Запрос является частью обычного разговора (small talk, chitchat).
     */
    @JsonAlias("CHIT CHAT") // Принимать "CHIT CHAT" как синоним
    CHITCHAT,

    /**
     * Запрос на семантический анализ OpenAPI спецификации.
     */
    OPENAPI_QUERY,

    /**
     * Явная инструкция сгенерировать фрагмент кода.
     */
    CODE_GENERATION,

    /**
     * Запрос на создание краткого содержания (summary).
     */
    SUMMARIZATION,

    /**
     * Не удалось определить намерение, используется как fallback.
     */
    UNKNOWN
}
