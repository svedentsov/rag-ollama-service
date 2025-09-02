package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от Агента-Приоритизатора.
 * Представляет собой готовый, приоритизированный план работ.
 *
 * @param sprintGoal Высокоуровневая цель спринта, заданная пользователем и подтвержденная AI.
 * @param tasks      Отсортированный список задач, готовых к выполнению.
 */
@Schema(description = "Приоритизированный бэклог, сгенерированный AI")
@JsonIgnoreProperties(ignoreUnknown = true)
public record PrioritizedBacklog(
        @Schema(description = "Высокоуровневая цель спринта")
        String sprintGoal,
        @Schema(description = "Отсортированный список задач для выполнения")
        List<PrioritizedTask> tasks
) {
    /**
     * DTO для представления одной приоритизированной задачи в бэклоге.
     * <p>
     * Этот record определен как вложенный, статический и публичный, чтобы
     * гарантировать его видимость и подчеркнуть сильную связь с
     * родительским DTO {@link PrioritizedBacklog}.
     *
     * @param title        Краткий, информативный заголовок для Jira-тикета.
     * @param description  Подробное описание задачи, включая контекст из отчетов.
     * @param sourceReport Имя отчета, из которого была взята проблема.
     * @param justification Обоснование от AI, почему эта задача важна для достижения цели спринта.
     * @param priority     Приоритет задачи (Highest, High, Medium).
     */
    @Schema(description = "Одна приоритизированная задача")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrioritizedTask(
            @Schema(description = "Название задачи для Jira")
            String title,
            @Schema(description = "Подробное описание и контекст")
            String description,
            @Schema(description = "Источник проблемы (например, 'TestDebtReport', 'SecurityScan')")
            String sourceReport,
            @Schema(description = "Обоснование от AI, почему эта задача важна")
            String justification,
            @Schema(description = "Приоритет")
            String priority
    ) {
    }
}
