package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO, представляющий семантический профиль пользовательского запроса.
 * Генерируется агентом QueryProfilerAgent для принятия решений о стратегии RAG.
 */
@Schema(description = "Семантический профиль пользовательского запроса")
@JsonIgnoreProperties(ignoreUnknown = true)
public record QueryProfile(
        @Schema(description = "Тип запроса")
        QueryType queryType,
        @Schema(description = "Требуемая широта поиска")
        SearchScope searchScope,
        @Schema(description = "Ожидаемый стиль ответа")
        AnswerStyle answerStyle
) {
    public enum QueryType {FACTUAL, ANALYTICAL, HOW_TO, CODE_RELATED, CHITCHAT}

    public enum SearchScope {NARROW, BROAD}

    public enum AnswerStyle {CONCISE, DETAILED}
}
