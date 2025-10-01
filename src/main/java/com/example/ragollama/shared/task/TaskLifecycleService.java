package com.example.ragollama.shared.task;

import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.shared.task.model.AsyncTask;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-реестр для управления жизненным циклом асинхронных, отменяемых задач.
 * Объединяет персистентное хранение состояния (в БД) и управление выполнением (в памяти).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskLifecycleService {

    private final AsyncTaskRepository taskRepository;

    @Getter
    private static class TaskRecord {
        private final CompletableFuture<?> future;
        private final Sinks.Many<UniversalResponse> sink;

        TaskRecord(CompletableFuture<?> future) {
            this.future = future;
            this.sink = Sinks.many().multicast().onBackpressureBuffer();
        }
    }

    private final Cache<UUID, TaskRecord> runningTasks = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> UUID register(CompletableFuture<T> taskFuture, UUID sessionId) {
        final UUID taskId = UUID.randomUUID();
        AsyncTask taskEntity = AsyncTask.builder()
                .id(taskId)
                .sessionId(sessionId)
                .status(TaskStatus.RUNNING)
                .build();
        taskRepository.save(taskEntity);

        final TaskRecord taskRecord = new TaskRecord(taskFuture);
        runningTasks.put(taskId, taskRecord);

        taskFuture.whenComplete((result, throwable) -> {
            // Используем self-инъекцию для выполнения в новой транзакции
            updateTaskStatusOnCompletion(taskId, throwable);
        });

        log.info("Новая задача {} для сессии {} зарегистрирована.", taskId, sessionId);
        return taskId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTaskStatusOnCompletion(UUID taskId, Throwable throwable) {
        AsyncTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return;

        TaskRecord record = runningTasks.getIfPresent(taskId);

        if (throwable != null) {
            Throwable cause = unwrapCompletionException(throwable);
            if (cause instanceof CancellationException) {
                task.setStatus(TaskStatus.CANCELLED);
                log.info("Задача {} была отменена.", taskId);
                record.getSink().tryEmitError(cause);
            } else {
                task.markAsFailed(cause.getMessage());
                log.warn("Задача {} завершилась с ошибкой: {}", taskId, cause.toString());
                record.getSink().tryEmitError(cause);
            }
        } else {
            task.setStatus(TaskStatus.COMPLETED);
            log.info("Задача {} успешно завершилась.", taskId);
            record.getSink().tryEmitComplete();
        }
        taskRepository.save(task);
        runningTasks.invalidate(taskId); // Очищаем из in-memory кэша
    }

    @Transactional
    public boolean cancel(UUID taskId) {
        AsyncTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != TaskStatus.RUNNING) {
            log.warn("Попытка отменить несуществующую или уже завершенную задачу {}", taskId);
            return false;
        }

        task.setStatus(TaskStatus.CANCELLATION_REQUESTED);
        taskRepository.save(task);

        TaskRecord taskRecord = runningTasks.getIfPresent(taskId);
        if (taskRecord != null) {
            log.warn("Запрошена отмена для задачи {}. Попытка прервать future.", taskId);
            return taskRecord.getFuture().cancel(true);
        }
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<TaskStatus> getStatus(UUID taskId) {
        return taskRepository.findById(taskId).map(AsyncTask::getStatus);
    }

    @Transactional(readOnly = true)
    public Optional<AsyncTask> getActiveTaskForSession(UUID sessionId) {
        return taskRepository.findBySessionIdAndStatus(sessionId, TaskStatus.RUNNING);
    }

    public Optional<Flux<UniversalResponse>> getTaskStream(UUID taskId) {
        return Optional.ofNullable(runningTasks.getIfPresent(taskId))
                .map(record -> record.getSink().asFlux());
    }

    public void emitEvent(UUID taskId, UniversalResponse event) {
        Optional.ofNullable(runningTasks.getIfPresent(taskId))
                .ifPresent(record -> record.getSink().tryEmitNext(event));
    }

    private static Throwable unwrapCompletionException(Throwable t) {
        if (t == null) return null;
        return (t instanceof java.util.concurrent.CompletionException && t.getCause() != null) ? t.getCause() : t;
    }
}
