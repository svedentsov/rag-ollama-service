package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * DTO для структурированного, иерархического плана тестирования.
 *
 * @param title    Заголовок плана.
 * @param summary  Краткое введение от AI.
 * @param sections Карта, где ключ - название секции (фичи), а значение - список пунктов.
 */
@Schema(description = "Иерархический план тестирования с контекстом")
@JsonIgnoreProperties(ignoreUnknown = true)
public record HierarchicalTestPlan(
        String title,
        String summary,
        Map<String, List<ChecklistItem>> sections
) {
}
