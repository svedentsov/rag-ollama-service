package com.example.ragollama.optimization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Приоритизированный бэклог, сгенерированный AI")
@JsonIgnoreProperties(ignoreUnknown = true)
public record PrioritizedBacklog(
        @Schema(description = "Высокоуровневая цель спринта")
        String sprintGoal,
        @Schema(description = "Отсортированный список задач для выполнения")
        List<PrioritizedTask> tasks
) {
}

@Schema(description = "Одна приоритизированная задача")
@JsonIgnoreProperties(ignoreUnknown = true)
record PrioritizedTask(
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
