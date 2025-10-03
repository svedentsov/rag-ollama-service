package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.shared.aop.ResilientDatabaseOperation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link ChatMessage}.
 * <p>
 * Кастомный метод `deleteBySessionId` был удален, так как каскадное удаление
 * теперь управляется через JPA-связь в `ChatSession`.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Находит последние N сообщений в сессии.
     * @param sessionId ID сессии.
     * @param pageable Объект пагинации для ограничения.
     * @return Список сообщений.
     */
    @ResilientDatabaseOperation
    @Query("SELECT m FROM ChatMessage m WHERE m.session.sessionId = :sessionId")
    List<ChatMessage> findBySessionId(UUID sessionId, Pageable pageable);

    /**
     * Находит самое последнее сообщение в сессии.
     * @param sessionId ID сессии.
     * @return Optional с последним сообщением.
     */
    @ResilientDatabaseOperation
    Optional<ChatMessage> findTopBySessionSessionIdOrderByCreatedAtDesc(UUID sessionId);

    @Override
    @ResilientDatabaseOperation
    <S extends ChatMessage> S save(S entity);

    @Override
    @ResilientDatabaseOperation
    Optional<ChatMessage> findById(UUID uuid);
}
