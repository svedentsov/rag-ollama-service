package com.example.ragollama.shared.task;

import com.example.ragollama.shared.task.model.AsyncTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями AsyncTask.
 */
@Repository
public interface AsyncTaskRepository extends JpaRepository<AsyncTask, UUID> {
    /**
     * Находит активную (в статусе RUNNING) задачу для указанной сессии.
     *
     * @param sessionId ID сессии.
     * @return Optional с найденной задачей.
     */
    Optional<AsyncTask> findBySessionIdAndStatus(UUID sessionId, TaskStatus status);

    /**
     * Находит все задачи, которые остались в статусе RUNNING, но были созданы
     * до момента запуска приложения. Это "зависшие" задачи.
     *
     * @return Список зависших задач.
     */
    @Query("SELECT t FROM AsyncTask t WHERE t.status = 'RUNNING'")
    List<AsyncTask> findStuckTasks();
}
