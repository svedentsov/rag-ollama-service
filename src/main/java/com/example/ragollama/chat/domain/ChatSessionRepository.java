package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByUserNameOrderByUpdatedAtDesc(String userName);
}
