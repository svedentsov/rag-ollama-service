package com.example.ragollama.shared.task;

import com.example.ragollama.shared.task.model.AsyncTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Фоновая задача для очистки "зависших" (stale) задач после перезапуска приложения.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskStaleRecordCleanupJob {

    private final AsyncTaskRepository taskRepository;

    /**
     * Выполняется один раз при старте приложения для немедленной очистки.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupOnStartup() {
        log.info("Запуск очистки 'зависших' задач при старте приложения...");
        cleanupStaleTasks();
    }

    /**
     * Выполняется периодически по расписанию.
     */
    @Scheduled(cron = "0 */15 * * * *") // Каждые 15 минут
    @Transactional
    public void cleanupPeriodically() {
        log.debug("Плановый запуск очистки 'зависших' задач...");
        cleanupStaleTasks();
    }

    private void cleanupStaleTasks() {
        List<AsyncTask> stuckTasks = taskRepository.findStuckTasks();
        if (!stuckTasks.isEmpty()) {
            log.warn("Обнаружено {} 'зависших' задач после перезапуска. Установка статуса FAILED.", stuckTasks.size());
            for (AsyncTask task : stuckTasks) {
                task.markAsFailed("Задача прервана из-за перезапуска/сбоя сервиса.");
            }
            taskRepository.saveAll(stuckTasks);
        }
    }
}
