package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO, представляющий семантический профиль пользовательского запроса.
 * <p>
 * Генерируется агентом {@link com.example.ragollama.optimization.QueryProfilerAgent}
 * для принятия решений о выборе оптимальной RAG-стратегии.
 */
@Schema(description = "Семантический профиль пользовательского запроса")
@JsonIgnoreProperties(ignoreUnknown = true)
public record QueryProfile(
        @Schema(description = "Тип запроса, определяющий его основное намерение.")
        QueryType queryType,
        @Schema(description = "Требуемая широта поиска для нахождения релевантной информации.")
        SearchScope searchScope,
        @Schema(description = "Ожидаемый стиль ответа от LLM.")
        AnswerStyle answerStyle
) {
    /**
     * Перечисление возможных типов запросов.
     */
    public enum QueryType {
        FACTUAL, ANALYTICAL, HOW_TO, CODE_RELATED, CHITCHAT
    }

    /**
     * Перечисление требуемой широты поиска.
     */
    public enum SearchScope {
        NARROW, BROAD
    }

    /**
     * Перечисление желаемого стиля ответа.
     */
    public enum AnswerStyle {
        CONCISE, DETAILED
    }
}
