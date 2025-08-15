package com.example.ragollama.repository;

import com.example.ragollama.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Находит последние сообщения для сессии с использованием пагинации.
     * Spring Data JPA автоматически сгенерирует SQL-запрос с LIMIT и OFFSET.
     *
     * @param sessionId ID сессии.
     * @param pageable  Объект с информацией о странице (номер, размер, сортировка).
     * @return Список сообщений.
     */
    List<ChatMessage> findBySessionId(UUID sessionId, Pageable pageable);
}
