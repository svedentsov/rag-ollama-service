package com.example.ragollama.agent.strategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для одной инициативы по рефакторингу.
 *
 * @param title           Название инициативы.
 * @param problemArea     Область кода, требующая внимания.
 * @param justification   Обоснование, почему этот рефакторинг важен, основанное на данных.
 * @param expectedOutcome Ожидаемый результат (бизнес-ценность).
 */
@Schema(description = "Одна предложенная инициатива по рефакторингу")
@JsonIgnoreProperties(ignoreUnknown = true)
public record RefactoringCandidate(
        String title,
        String problemArea,
        String justification,
        String expectedOutcome
) {
}
