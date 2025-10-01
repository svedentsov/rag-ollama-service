package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByUserNameOrderByUpdatedAtDesc(String userName);

    /**
     * Находит сессию по ID и накладывает пессимистическую блокировку на запись.
     *
     * @param sessionId ID сессии.
     * @return Optional с заблокированной сущностью.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ChatSession s WHERE s.sessionId = :sessionId")
    Optional<ChatSession> findByIdWithLock(UUID sessionId);
}
