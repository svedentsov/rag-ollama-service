package com.example.ragollama.shared.task;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Сервис для регистрации CompletableFuture задач с возможностью отмены по UUID.
 * Хранит записи в Guava Cache с автоматическим удалением через 5 минут после записи.
 * Эта версия является потокобезопасной и использует AtomicReference для управления состоянием.
 */
@Slf4j
@Service
public class CancellableTaskService {

    private final Cache<UUID, TaskRecord> runningTasks;

    public CancellableTaskService() {
        this.runningTasks = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    /**
     * Зарегистрировать задачу и получить её UUID.
     *
     * @param taskFuture future задачи
     * @param <T>        тип результата
     * @return UUID задачи
     */
    public <T> UUID register(CompletableFuture<T> taskFuture) {
        final UUID taskId = UUID.randomUUID();
        final TaskRecord taskRecord = new TaskRecord(taskFuture);
        runningTasks.put(taskId, taskRecord);
        taskFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                Throwable cause = unwrapCompletionException(throwable);
                if (cause instanceof CancellationException) {
                    taskRecord.status.compareAndSet(TaskStatus.RUNNING, TaskStatus.CANCELLED);
                    log.info("Задача {} была отменена (через throwable) и ее статус обновлен.", taskId);
                } else {
                    taskRecord.status.compareAndSet(TaskStatus.RUNNING, TaskStatus.FAILED);
                    log.warn("Задача {} завершилась с ошибкой: {}", taskId, cause.toString());
                }
            } else {
                boolean completed = taskRecord.status.compareAndSet(TaskStatus.RUNNING, TaskStatus.COMPLETED);
                if (completed) {
                    log.info("Задача {} успешно завершилась.", taskId);
                }
            }
        });
        log.info("Новая задача зарегистрирована с ID: {}", taskId);
        return taskId;
    }

    /**
     * Попытаться отменить задачу по её UUID.
     *
     * @param taskId UUID задачи
     * @return true если запись найдена и была предпринята попытка отмены, false — если запись отсутствует
     */
    public boolean cancel(UUID taskId) {
        TaskRecord taskRecord = runningTasks.getIfPresent(taskId);
        if (taskRecord == null) {
            log.warn("Попытка отменить несуществующую задачу с ID: {}", taskId);
            return false;
        }
        if (taskRecord.status.compareAndSet(TaskStatus.RUNNING, TaskStatus.CANCELLED)) {
            log.warn("Получен запрос на отмену задачи {}. Статус изменен на CANCELLED.", taskId);
            boolean canceled = taskRecord.getFuture().cancel(true);
            log.info("Попытка отмены future для задачи {} выполнена (future.cancel(true) вернул {}).", taskId, canceled);
            return true;
        } else {
            log.info("Задача {} уже находится в терминальном статусе ({}), отмена не требуется.", taskId, taskRecord.getStatus());
            return true;
        }
    }

    public Optional<CompletableFuture<?>> getTask(UUID taskId) {
        return Optional.ofNullable(runningTasks.getIfPresent(taskId))
                .map(TaskRecord::getFuture);
    }

    public Optional<TaskStatus> getStatus(UUID taskId) {
        return Optional.ofNullable(runningTasks.getIfPresent(taskId))
                .map(TaskRecord::getStatus);
    }

    private static Throwable unwrapCompletionException(Throwable t) {
        if (t == null) return null;
        return (t instanceof java.util.concurrent.CompletionException && t.getCause() != null) ? t.getCause() : t;
    }

    /**
     * Запись о задаче, хранит future и потокобезопасный статус.
     */
    private static class TaskRecord {
        @Getter
        private final CompletableFuture<?> future;
        private final AtomicReference<TaskStatus> status;

        TaskRecord(CompletableFuture<?> future) {
            this.future = future;
            this.status = new AtomicReference<>(TaskStatus.RUNNING);
        }

        public TaskStatus getStatus() {
            return status.get();
        }
    }
}
