package com.example.ragollama.repository;

import com.example.ragollama.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA репозиторий для сущности {@link ChatMessage}.
 * <p>
 * Предоставляет стандартные CRUD-операции (Create, Read, Update, Delete)
 * для таблицы {@code chat_messages} и позволяет определять кастомные
 * методы для выполнения запросов к базе данных.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Находит все сообщения для указанной сессии, упорядоченные по времени создания (от старых к новым).
     * <p>
     * Этот метод может быть использован в будущем для восстановления контекста диалога
     * путем загрузки предыдущих сообщений и передачи их в LLM.
     *
     * @param sessionId Уникальный идентификатор сессии чата.
     * @return Список {@link ChatMessage} в хронологическом порядке.
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
