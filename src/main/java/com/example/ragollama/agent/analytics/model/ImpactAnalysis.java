package com.example.ragollama.agent.analytics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO для представления одного результата анализа влияния (Impact Analysis).
 *
 * @param impactedComponent Компонент, который может быть затронут (например, "API Consumer", "Database Schema").
 * @param impactDescription Описание того, как именно изменение влияет на компонент.
 * @param severity          Оценка серьезности влияния ("High", "Medium", "Low").
 * @param sourceFile        Файл, изменение в котором вызвало данный импакт.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImpactAnalysis(
        String impactedComponent,
        String impactDescription,
        String severity,
        String sourceFile
) {
}
