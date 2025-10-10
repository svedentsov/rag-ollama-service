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
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-реестр для управления жизненным циклом асинхронных, отменяемых задач.
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

    /**
     * Регистрирует новую асинхронную задачу в базе данных и в in-memory реестре.
     *
     * @param taskFuture {@link CompletableFuture}, представляющий выполнение задачи.
     * @param sessionId  ID сессии, к которой привязана задача.
     * @return {@link Mono} с ID созданной задачи.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> Mono<UUID> register(CompletableFuture<T> taskFuture, UUID sessionId) {
        AsyncTask taskEntity = AsyncTask.builder()
                .sessionId(sessionId)
                .status(TaskStatus.RUNNING)
                .build();

        return taskRepository.save(taskEntity)
                .map(savedTask -> {
                    final UUID taskId = savedTask.getId();
                    final TaskRecord taskRecord = new TaskRecord(taskFuture);
                    runningTasks.put(taskId, taskRecord);

                    taskFuture.whenComplete((result, throwable) ->
                            updateTaskStatusOnCompletion(taskId, throwable).subscribe()
                    );
                    log.info("Новая задача {} для сессии {} зарегистрирована.", taskId, sessionId);
                    return taskId;
                });
    }

    /**
     * Обновляет статус задачи в БД по ее завершении (успешном или с ошибкой).
     *
     * @param taskId    ID задачи.
     * @param throwable Исключение, если задача завершилась с ошибкой, иначе null.
     * @return {@link Mono<Void>}, завершающийся после обновления.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> updateTaskStatusOnCompletion(UUID taskId, Throwable throwable) {
        return taskRepository.findById(taskId)
                .flatMap(task -> {
                    TaskRecord record = runningTasks.getIfPresent(taskId);
                    if (throwable != null) {
                        Throwable cause = unwrapCompletionException(throwable);
                        if (cause instanceof CancellationException) {
                            task.setStatus(TaskStatus.CANCELLED);
                            log.info("Задача {} была отменена.", taskId);
                            if (record != null) record.getSink().tryEmitError(cause);
                        } else {
                            task.markAsFailed(cause.getMessage());
                            log.warn("Задача {} завершилась с ошибкой: {}", taskId, cause.toString());
                            if (record != null) record.getSink().tryEmitError(cause);
                        }
                    } else {
                        task.setStatus(TaskStatus.COMPLETED);
                        log.info("Задача {} успешно завершилась.", taskId);
                        if (record != null) record.getSink().tryEmitComplete();
                    }
                    runningTasks.invalidate(taskId);
                    return taskRepository.save(task);
                }).then();
    }

    /**
     * Запрашивает отмену выполняющейся задачи.
     *
     * @param taskId ID задачи.
     * @return {@link Mono<Void>}, завершающийся после запроса на отмену.
     */
    @Transactional
    public Mono<Void> cancel(UUID taskId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getStatus() == TaskStatus.RUNNING)
                .flatMap(task -> {
                    task.setStatus(TaskStatus.CANCELLATION_REQUESTED);
                    return taskRepository.save(task);
                })
                .doOnSuccess(task -> {
                    if (task != null) {
                        TaskRecord taskRecord = runningTasks.getIfPresent(taskId);
                        if (taskRecord != null) {
                            log.warn("Запрошена отмена для задачи {}. Попытка прервать future.", taskId);
                            taskRecord.getFuture().cancel(true);
                        }
                    }
                }).then();
    }

    @Transactional(readOnly = true)
    public Mono<TaskStatus> getStatus(UUID taskId) {
        return taskRepository.findById(taskId).map(AsyncTask::getStatus);
    }

    @Transactional(readOnly = true)
    public Mono<AsyncTask> getActiveTaskForSession(UUID sessionId) {
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
