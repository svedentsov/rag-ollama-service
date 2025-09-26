package com.example.ragollama.shared.task;

import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class CancellableTaskService {

    private final Cache<UUID, TaskRecord> runningTasks;

    public CancellableTaskService() {
        this.runningTasks = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15))
                .build();
    }

    @Getter
    private static class TaskRecord {
        private final CompletableFuture<?> future;
        private final AtomicReference<TaskStatus> status;
        private final Sinks.Many<UniversalResponse> sink;

        TaskRecord(CompletableFuture<?> future) {
            this.future = future;
            this.status = new AtomicReference<>(TaskStatus.RUNNING);
            this.sink = Sinks.many().multicast().onBackpressureBuffer();
        }
    }

    public <T> UUID register(CompletableFuture<T> taskFuture) {
        final UUID taskId = UUID.randomUUID();
        final TaskRecord taskRecord = new TaskRecord(taskFuture);
        runningTasks.put(taskId, taskRecord);

        taskFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                Throwable cause = unwrapCompletionException(throwable);
                if (cause instanceof CancellationException) {
                    taskRecord.status.set(TaskStatus.CANCELLED);
                    log.info("Задача {} была отменена (через throwable) и ее статус обновлен.", taskId);
                    taskRecord.getSink().tryEmitError(cause);
                } else {
                    taskRecord.status.set(TaskStatus.FAILED);
                    log.warn("Задача {} завершилась с ошибкой: {}", taskId, cause.toString());
                    taskRecord.getSink().tryEmitError(cause);
                }
            } else {
                taskRecord.status.compareAndSet(TaskStatus.RUNNING, TaskStatus.COMPLETED);
                log.info("Задача {} успешно завершилась.", taskId);
                taskRecord.getSink().tryEmitComplete();
            }
        });
        log.info("Новая задача зарегистрирована с ID: {}", taskId);
        return taskId;
    }

    public Optional<Flux<UniversalResponse>> getTaskStream(UUID taskId) {
        return Optional.ofNullable(runningTasks.getIfPresent(taskId))
                .map(record -> record.getSink().asFlux());
    }

    public void emitEvent(UUID taskId, UniversalResponse event) {
        Optional.ofNullable(runningTasks.getIfPresent(taskId))
                .ifPresent(record -> {
                    log.trace("Отправка события типа {} для задачи {}", event.getClass().getSimpleName(), taskId);
                    record.getSink().tryEmitNext(event);
                });
    }

    public boolean cancel(UUID taskId) {
        TaskRecord taskRecord = runningTasks.getIfPresent(taskId);
        if (taskRecord == null) {
            log.warn("Попытка отменить несуществующую или уже завершенную задачу с ID: {}", taskId);
            return false;
        }

        if (taskRecord.status.compareAndSet(TaskStatus.RUNNING, TaskStatus.CANCELLATION_REQUESTED)) {
            log.warn("Запрошена отмена для задачи {}. Попытка прервать future.", taskId);
            boolean canceled = taskRecord.getFuture().cancel(true);
            log.info("Результат future.cancel(true) для задачи {}: {}", taskId, canceled);
            return true;
        } else {
            log.info("Задача {} уже не в статусе RUNNING (текущий: {}), отмена не требуется.", taskId, taskRecord.status.get());
            return false;
        }
    }

    public Optional<CompletableFuture<?>> getTask(UUID taskId) {
        return Optional.ofNullable(runningTasks.getIfPresent(taskId))
                .map(TaskRecord::getFuture);
    }

    public Optional<TaskStatus> getStatus(UUID taskId) {
        return Optional.ofNullable(runningTasks.getIfPresent(taskId))
                .map(record -> record.getStatus().get());
    }

    private static Throwable unwrapCompletionException(Throwable t) {
        if (t == null) return null;
        return (t instanceof java.util.concurrent.CompletionException && t.getCause() != null) ? t.getCause() : t;
    }
}
