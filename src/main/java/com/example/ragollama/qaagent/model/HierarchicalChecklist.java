package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * DTO для структурированного, многосекционного чек-листа.
 *
 * @param title    Заголовок чек-листа.
 * @param summary  Краткое введение от AI.
 * @param sections Карта, где ключ - название секции, а значение - список пунктов.
 */
@Schema(description = "Структурированный, многосекционный чек-лист")
@JsonIgnoreProperties(ignoreUnknown = true)
public record HierarchicalChecklist(
        String title,
        String summary,
        Map<String, List<String>> sections
) {
}
