package com.example.ragollama.qaagent.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO, представляющий собой единую, агрегированную панель мониторинга
 * ("дашборд") состояния качества всего проекта.
 * <p>
 * Эта модель данных является "продуктом" работы самого высокоуровневого
 * агента-агрегатора и служит источником истины для любого UI или
 * системы отчетности.
 */
@Schema(description = "Сводный дашборд состояния QA-процессов")
@Data
@Builder
public class QaDashboard {

    /**
     * Общий вердикт о готовности текущих изменений к релизу.
     */
    @Schema(description = "Финальный отчет о готовности релиза")
    private ReleaseReadinessReport releaseReadiness;

    /**
     * Приоритизированный список файлов, представляющих наибольший риск.
     * Выборка из полной матрицы рисков.
     */
    @Schema(description = "Топ-5 самых рискованных файлов в текущих изменениях")
    private List<RiskMatrixItem> topRisks;

    /**
     * Ключевые показатели технического долга в тестах.
     */
    @Schema(description = "Ключевые метрики тестового технического долга")
    private TestDebtSummary testDebtSummary;

    /**
     * Внутренний DTO для ключевых метрик техдолга.
     */
    @Schema(description = "Сводка по тестовому техническому долгу")
    @Data
    @Builder
    public static class TestDebtSummary {
        private int flakyTestCount;
        private int slowTestCount;
        private int missingCoverageFileCount;
        private int disabledTestCount;
    }
}
