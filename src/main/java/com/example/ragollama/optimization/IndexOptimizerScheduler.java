package com.example.ragollama.optimization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Планировщик, отвечающий за периодический запуск агента оптимизации индекса.
 * <p>
 * Этот компонент отделен от основной логики оптимизации и занимается
 * исключительно запуском задачи по расписанию, заданному в {@code application.yml}.
 * Активируется свойством {@code app.optimization.index.enabled=true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.optimization.index.enabled", havingValue = "true")
public class IndexOptimizerScheduler {

    private final IndexOptimizerService indexOptimizerService;

    /**
     * Запускает асинхронную задачу оптимизации индекса по cron-расписанию.
     * Расписание настраивается через свойство `app.optimization.index.cron`.
     */
    @Scheduled(cron = "${app.optimization.index.cron}")
    public void runScheduledIndexOptimization() {
        log.info("Планировщик запускает фоновую задачу оптимизации индекса...");
        indexOptimizerService.runOptimizationAsync();
    }
}
