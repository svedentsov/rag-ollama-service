package com.example.ragollama.agent.buganalysis.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального, обогащенного отчета о баге.
 *
 * @param structuredReport  Структурированный отчет (шаги, ОР, ФР).
 * @param duplicateAnalysis Результат анализа на дубликаты.
 */
@Schema(description = "Обогащенный отчет о баге")
public record EnhancedBugReport(
        BugReportSummary structuredReport,
        DuplicateAnalysis duplicateAnalysis
) {
    /**
     * Результат анализа на дубликаты.
     */
    @Schema(description = "Результат анализа на дубликаты")
    public record DuplicateAnalysis(
            boolean isDuplicate,
            List<String> duplicateCandidates
    ) {
    }
}
