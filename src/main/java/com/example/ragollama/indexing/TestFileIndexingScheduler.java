package com.example.ragollama.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Планировщик для периодического запуска задачи индексации тест-кейсов.
 * <p>
 * Активируется свойством {@code app.indexing.test-files.scheduler.enabled=true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.indexing.test-files.scheduler.enabled", havingValue = "true")
public class TestFileIndexingScheduler {

    private final TestFileIndexerService indexerService;

    /**
     * Запускает полную индексацию директории с тестами по расписанию.
     * <p>
     * Расписание задается в `application.yml` через свойство `app.indexing.test-files.scheduler.cron`.
     */
    @Scheduled(cron = "${app.indexing.test-files.scheduler.cron}")
    public void runScheduledIndexing() {
        log.info("Планировщик запускает фоновую задачу индексации тест-кейсов...");
        indexerService.indexAllTestFilesAsync();
    }
}
