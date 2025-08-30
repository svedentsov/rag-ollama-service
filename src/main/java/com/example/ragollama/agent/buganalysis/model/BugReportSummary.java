package com.example.ragollama.agent.buganalysis.model;

import com.example.ragollama.agent.buganalysis.domain.BugReportSummarizerAgent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для структурированного представления баг-репорта.
 * <p>
 * Этот объект является "продуктом" работы агента {@link BugReportSummarizerAgent},
 * который извлекает эти данные из неструктурированного текста.
 *
 * @param title            Краткий, стандартизированный заголовок проблемы.
 * @param stepsToReproduce Пошаговая инструкция для воспроизведения бага.
 * @param expectedBehavior Описание того, как система должна была себя вести.
 * @param actualBehavior   Описание того, что произошло на самом деле.
 */
@Schema(description = "Структурированное представление баг-репорта")
@JsonIgnoreProperties(ignoreUnknown = true)
public record BugReportSummary(
        @Schema(description = "Краткий заголовок проблемы")
        String title,
        @Schema(description = "Пошаговая инструкция для воспроизведения")
        List<String> stepsToReproduce,
        @Schema(description = "Ожидаемое поведение системы")
        String expectedBehavior,
        @Schema(description = "Фактическое поведение системы")
        String actualBehavior
) {
}
