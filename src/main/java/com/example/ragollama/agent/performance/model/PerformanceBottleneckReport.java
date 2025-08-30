package com.example.ragollama.agent.performance.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для полного отчета об анализе узких мест в производительности.
 *
 * @param findings Список всех обнаруженных проблем.
 */
@Schema(description = "Отчет о потенциальных узких местах в производительности")
public record PerformanceBottleneckReport(
        List<PerformanceFinding> findings
) {
}
