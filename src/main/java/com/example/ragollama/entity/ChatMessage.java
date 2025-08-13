package com.example.ragollama.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность JPA, представляющая одно сообщение в истории чата.
 * <p>
 * Каждая запись в таблице {@code chat_messages} соответствует одному
 * сообщению от пользователя или ассистента в рамках определенной сессии.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    /**
     * Уникальный идентификатор сообщения.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Идентификатор сессии чата, к которой относится сообщение.
     * Позволяет группировать сообщения одного диалога.
     */
    @NotNull
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    /**
     * Роль отправителя сообщения (пользователь или ассистент).
     *
     * @see MessageRole
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, updatable = false)
    private MessageRole role;

    /**
     * Текстовое содержимое сообщения.
     */
    @NotNull
    @Lob // Large Object, указывает, что поле может хранить большой объем текста.
    @Column(name = "content", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Временная метка создания сообщения.
     */
    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
