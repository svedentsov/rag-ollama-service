package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatSession;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Реактивный репозиторий для управления сущностями ChatSession.
 */
@Repository
public interface ChatSessionRepository extends ReactiveCrudRepository<ChatSession, UUID> {
    Flux<ChatSession> findByUserNameOrderByUpdatedAtDesc(String userName);

    /**
     * Находит сессию по ID.
     * В R2DBC нет поддержки пессимистичных блокировок на уровне Spring Data.
     * Оптимистичные блокировки реализуются через @Version.
     *
     * @param sessionId ID сессии.
     * @return Mono с сущностью.
     */
    @Query("SELECT * FROM chat_sessions WHERE session_id = :sessionId")
    Mono<ChatSession> findByIdWithLock(UUID sessionId);
}
