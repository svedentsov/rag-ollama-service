package com.example.ragollama.qaagent.copilot;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Сервис для управления жизненным циклом сессий QA Copilot.
 * <p>
 * Использует Spring Cache Abstraction для хранения состояний сессий в памяти.
 * Каждая сессия автоматически удаляется из кэша через 30 минут неактивности.
 */
@Service
@RequiredArgsConstructor
public class CopilotSessionService {

    /**
     * Получает существующую сессию из кэша.
     * Если сессия не найдена, она будет создана с помощью метода `createSession`.
     *
     * @param sessionId ID сессии.
     * @return Объект {@link CopilotSession}.
     */
    @Cacheable(value = "copilot_sessions", key = "#sessionId")
    public CopilotSession getSession(UUID sessionId) {
        // Этот метод вызывается только при промахе кэша
        return new CopilotSession();
    }

    /**
     * Обновляет состояние сессии в кэше.
     *
     * @param sessionId ID сессии.
     * @param session   Обновленный объект сессии.
     * @return Тот же объект сессии, который был помещен в кэш.
     */
    @CachePut(value = "copilot_sessions", key = "#sessionId")
    public CopilotSession updateSession(UUID sessionId, CopilotSession session) {
        return session;
    }

    /**
     * Возвращает ID сессии из запроса или генерирует новый.
     *
     * @param sessionId Опциональный ID из DTO.
     * @return Не-null UUID.
     */
    public UUID getOrCreateSessionId(UUID sessionId) {
        return (sessionId != null) ? sessionId : UUID.randomUUID();
    }
}
