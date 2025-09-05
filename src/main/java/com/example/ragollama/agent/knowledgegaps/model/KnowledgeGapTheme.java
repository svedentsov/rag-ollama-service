package com.example.ragollama.agent.knowledgegaps.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одной темы (кластера) пробелов в знаниях.
 *
 * @param theme                 Название темы, сгенерированное AI.
 * @param frequency             Количество пользовательских запросов, относящихся к этой теме.
 * @param userNeed              Описание потребности пользователя, стоящей за этими запросами.
 * @param suggestedArticleTitle Предлагаемый заголовок для новой статьи в базе знаний.
 */
@Schema(description = "Одна тема (кластер) пробелов в знаниях")
@JsonIgnoreProperties(ignoreUnknown = true)
public record KnowledgeGapTheme(
        String theme,
        int frequency,
        String userNeed,
        String suggestedArticleTitle
) {
}
