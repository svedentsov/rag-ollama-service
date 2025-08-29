package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одного пункта в иерархическом чек-листе.
 *
 * @param checkItem Текст проверки (например, "Проверить успешную авторизацию").
 * @param context   Дополнительный контекст для тестировщика (требования, API-контракт).
 */
@Schema(description = "Один пункт в иерархическом чек-листе с контекстом")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChecklistItem(
        String checkItem,
        String context
) {
}
