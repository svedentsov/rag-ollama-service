package com.example.ragollama.shared.task;

import com.example.ragollama.shared.task.model.AsyncTask;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Реактивный репозиторий для AsyncTask.
 */
@Repository
public interface AsyncTaskRepository extends ReactiveCrudRepository<AsyncTask, UUID> {

    /**
     * Находит активную задачу для сессии.
     *
     * @param sessionId ID сессии.
     * @param status    Статус.
     * @return Mono с задачей.
     */
    Mono<AsyncTask> findBySessionIdAndStatus(UUID sessionId, TaskStatus status);

    /**
     * Находит все "зависшие" задачи.
     *
     * @return Поток задач.
     */
    @Query("SELECT * FROM async_tasks WHERE status = 'RUNNING'")
    Flux<AsyncTask> findStuckTasks();
}
