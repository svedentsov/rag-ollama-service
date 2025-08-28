package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для финального ответа от Knowledge Aggregator.
 *
 * @param naturalLanguageResponse Ответ на вопрос пользователя, сгенерированный AI.
 * @param cypherQuery             Cypher-запрос, который был сгенерирован и выполнен.
 * @param rawGraphResult          "Сырой" результат выполнения запроса из графовой БД.
 */
@Schema(description = "Ответ от Агрегатора Знаний")
@JsonIgnoreProperties(ignoreUnknown = true)
public record KnowledgeGraphResponse(
        @Schema(description = "Ответ на естественном языке")
        String naturalLanguageResponse,
        @Schema(description = "Сгенерированный Cypher-запрос")
        String cypherQuery,
        @Schema(description = "Сырой результат из графовой БД")
        Object rawGraphResult
) {
}
