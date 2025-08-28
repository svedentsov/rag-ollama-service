package com.example.ragollama.qaagent.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одной просканированной зависимости.
 *
 * @param group   Maven-группа.
 * @param name    Имя артефакта.
 * @param version Версия.
 * @param license Обнаруженная лицензия.
 */
@Schema(description = "Одна просканированная зависимость")
public record ScannedDependency(
        String group,
        String name,
        String version,
        String license
) {
}
