package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * DTO для структурированного представления баг-репорта.
 *
 * @param title            Краткий, стандартизированный заголовок проблемы.
 * @param stepsToReproduce Пошаговая инструкция для воспроизведения бага.
 * @param expectedBehavior Описание того, как система должна была себя вести.
 * @param actualBehavior   Описание того, что произошло на самом деле.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BugReportSummary(
        String title,
        List<String> stepsToReproduce,
        String expectedBehavior,
        String actualBehavior
) {
}
