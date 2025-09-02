package com.example.ragollama.agent.strategy.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO, агрегирующий ключевые показатели здоровья для одного проекта.
 * <p>
 * Является "карточкой проекта" для высокоуровневого анализа.
 *
 * @param projectId           Уникальный идентификатор проекта.
 * @param projectName         Человекочитаемое имя проекта.
 * @param overallHealthScore  Общая оценка здоровья (1-100), рассчитанная по совокупности факторов.
 * @param testPassRate        Средний процент прохождения тестов.
 * @param codeComplexityScore Средняя цикломатическая сложность (в данной реализации - мок).
 * @param criticalAlertsCount Количество критических алертов (уязвимостей, flaky-тестов).
 */
@Schema(description = "Ключевые показатели здоровья для одного проекта")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectHealthSummary {
    private String projectId;
    private String projectName;
    private int overallHealthScore;
    private double testPassRate;
    private double codeComplexityScore;
    private int criticalAlertsCount;
}
