package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одной конкретной проблемы производительности.
 *
 * @param filePath        Путь к файлу.
 * @param lines           Диапазон строк, где обнаружена проблема.
 * @param antiPatternType Тип обнаруженного анти-паттерна (например, "DB_CALL_IN_LOOP").
 * @param severity        Серьезность ("Critical", "Major", "Minor").
 * @param explanation     Подробное объяснение, почему это проблема.
 * @param suggestedFix    Конкретная рекомендация по исправлению с примером кода.
 */
@Schema(description = "Описание одной найденной проблемы производительности")
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerformanceFinding(
        String filePath,
        String lines,
        String antiPatternType,
        String severity,
        String explanation,
        String suggestedFix
) {
}
