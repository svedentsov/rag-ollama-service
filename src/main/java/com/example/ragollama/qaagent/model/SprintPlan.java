package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от AI Sprint Planner.
 *
 * @param sprintGoal    Высокоуровневая цель спринта.
 * @param sprintBacklog Список задач, выбранных для включения в спринт.
 */
@Schema(description = "План на следующий спринт, сгенерированный AI")
@JsonIgnoreProperties(ignoreUnknown = true)
public record SprintPlan(
        String sprintGoal,
        List<SprintTask> sprintBacklog
) {
}
