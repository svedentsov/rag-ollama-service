package com.example.ragollama.repository;

import com.example.ragollama.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA репозиторий для сущности {@link ChatMessage}.
 * <p>
 * Предоставляет стандартные CRUD-операции и позволяет определять
 * кастомные методы для запросов к таблице `chat_messages`.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Находит все сообщения для указанной сессии, упорядоченные по времени создания.
     * Этот метод может быть использован в будущем для загрузки истории диалога
     * и передачи ее в контекст LLM.
     *
     * @param sessionId ID сессии чата.
     * @return Список сообщений в хронологическом порядке.
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
