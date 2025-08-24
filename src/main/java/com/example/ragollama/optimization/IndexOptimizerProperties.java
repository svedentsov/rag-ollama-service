package com.example.ragollama.optimization;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Типобезопасная конфигурация для агента оптимизации индекса.
 * <p>
 * Позволяет гибко управлять работой агента через {@code application.yml},
 * включая его включение/выключение и расписание запуска.
 *
 * @EnableConfigurationProperties в главном классе приложения.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.optimization.index")
public class IndexOptimizerProperties {

    /**
     * Включает или отключает запуск оптимизатора по расписанию.
     */
    private boolean enabled = false;

    /**
     * Cron-выражение для запуска задачи оптимизации.
     * По умолчанию - каждый день в 4 часа утра.
     */
    private String cron = "0 0 4 * * ?";

    /**
     * Конфигурация для этапа обнаружения и удаления устаревших документов.
     */
    private StaleDocumentDetection staleDocumentDetection = new StaleDocumentDetection();

    @Getter
    @Setter
    public static class StaleDocumentDetection {
        /**
         * Включает или отключает этап очистки "осиротевших" чанков.
         */
        private boolean enabled = true;
    }
}
