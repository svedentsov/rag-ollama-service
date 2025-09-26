package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.shared.aop.ResilientDatabaseOperation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    @ResilientDatabaseOperation
    List<ChatMessage> findBySessionId(UUID sessionId, Pageable pageable);

    @ResilientDatabaseOperation
    @Override
    <S extends ChatMessage> S save(S entity);

    /**
     * !!! ИЗМЕНЕНИЕ: Добавлен новый метод.
     * Удаляет все сообщения, принадлежащие одной сессии.
     * Этот метод необходим для каскадного удаления: перед тем как удалить
     * сессию чата, нужно удалить все связанные с ней сообщения.
     *
     * @param sessionId ID сессии, сообщения которой нужно удалить.
     */
    @Modifying
    @ResilientDatabaseOperation
    void deleteBySessionId(UUID sessionId);
}
