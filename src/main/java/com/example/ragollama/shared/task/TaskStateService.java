package com.example.ragollama.shared.task;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для отслеживания связи между сессией чата и активной асинхронной задачей.
 * Использует кеш для временного хранения этой связи.
 */
@Service
@Slf4j
public class TaskStateService {

    // Связь sessionId -> taskId
    private final Cache<UUID, UUID> sessionToTaskCache;

    /**
     * Конструктор, инициализирующий кэш для хранения связей.
     */
    public TaskStateService() {
        this.sessionToTaskCache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15)) // Задачи не должны висеть вечно
                .build();
    }

    /**
     * Регистрирует, что для данной сессии запущена задача.
     *
     * @param sessionId ID сессии
     * @param taskId    ID задачи
     */
    public void registerSessionTask(UUID sessionId, UUID taskId) {
        sessionToTaskCache.put(sessionId, taskId);
        log.debug("Связана сессия {} с задачей {}", sessionId, taskId);
    }

    /**
     * Удаляет связь сессии с задачей (когда задача завершена).
     *
     * @param sessionId ID сессии
     */
    public void clearSessionTask(UUID sessionId) {
        sessionToTaskCache.invalidate(sessionId);
        log.debug("Связь для сессии {} удалена.", sessionId);
    }

    /**
     * Находит активную задачу для сессии.
     *
     * @param sessionId ID сессии
     * @return Optional с ID задачи, если она есть
     */
    public Optional<UUID> getActiveTaskIdForSession(UUID sessionId) {
        return Optional.ofNullable(sessionToTaskCache.getIfPresent(sessionId));
    }
}
