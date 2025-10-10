package com.example.ragollama.shared.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Фоновая задача для очистки "зависших" задач, адаптированная для R2DBC.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskStaleRecordCleanupJob {

    private final AsyncTaskRepository taskRepository;

    /**
     * Запускает очистку при старте приложения.
     * <p>
     * {@code @EventListener} является блокирующим по своей природе. Мы вызываем
     * реактивный метод {@code cleanupStaleTasks()} и используем {@code .block()}
     * для синхронного ожидания его завершения, что является допустимым
     * и прагматичным решением для задачи, выполняемой один раз при запуске.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void cleanupOnStartup() {
        log.info("Запуск очистки 'зависших' задач при старте приложения...");
        cleanupStaleTasks().block(); // Блокируемся, так как это разовая задача при старте
    }

    /**
     * Запускает очистку по расписанию.
     * <p>
     * {@code @Scheduled} методы могут быть типа `void`. Мы вызываем реактивный
     * метод и подписываемся на него с помощью {@code .subscribe()}, чтобы
     * запустить выполнение в фоновом режиме.
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void cleanupPeriodically() {
        log.debug("Плановый запуск очистки 'зависших' задач...");
        cleanupStaleTasks().subscribe(
                null, // onNext - не требуется
                error -> log.error("Ошибка при плановой очистке 'зависших' задач.", error)
        );
    }

    /**
     * Основная логика очистки, инкапсулированная в транзакционном, реактивном методе.
     *
     * @return {@link Mono<Void>}, который завершается после выполнения всех операций.
     */
    @Transactional
    public Mono<Void> cleanupStaleTasks() {
        return taskRepository.findStuckTasks()
                .collectList()
                .filter(stuckTasks -> !stuckTasks.isEmpty())
                .flatMap(stuckTasks -> {
                    log.warn("Обнаружено {} 'зависших' задач. Установка статуса FAILED.", stuckTasks.size());
                    stuckTasks.forEach(task -> task.markAsFailed("Задача прервана из-за перезапуска/сбоя сервиса."));
                    return taskRepository.saveAll(stuckTasks).then();
                });
    }
}
