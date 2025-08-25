package com.example.ragollama.qaagent.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO для представления отчета о нестабильных тестах.
 *
 * @param generatedAt        Время генерации отчета.
 * @param analysisPeriodDays Период анализа в днях.
 * @param flakinessThreshold Порог нестабильности, использованный для анализа.
 * @param flakyTests         Список тестов, превысивших порог.
 */
public record FlakinessReport(
        OffsetDateTime generatedAt,
        int analysisPeriodDays,
        double flakinessThreshold,
        List<FlakyTestInfo> flakyTests
) {
}
