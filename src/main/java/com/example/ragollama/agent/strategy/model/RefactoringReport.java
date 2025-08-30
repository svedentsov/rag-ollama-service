package com.example.ragollama.agent.strategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для отчета от AI Tech Lead с предложениями по рефакторингу.
 *
 * @param summary               Общее заключение о здоровье кодовой базы.
 * @param refactoringCandidates Список предложенных инициатив по рефакторингу.
 */
@Schema(description = "Стратегический отчет с предложениями по рефакторингу")
@JsonIgnoreProperties(ignoreUnknown = true)
public record RefactoringReport(
        String summary,
        List<RefactoringCandidate> refactoringCandidates
) {
}
