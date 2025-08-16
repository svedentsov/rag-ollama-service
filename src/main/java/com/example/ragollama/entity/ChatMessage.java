package com.example.ragollama.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность JPA, представляющая одно сообщение в истории чата.
 * Каждая запись в таблице {@code chat_messages} соответствует одному
 * сообщению от пользователя или ассистента в рамках определенной сессии.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "content") // Исключаем большое текстовое поле из toString()
@EqualsAndHashCode(of = "id") // Реализуем equals/hashCode только по ID
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    /**
     * Уникальный идентификатор сообщения. Является основой для equals и hashCode.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Идентификатор сессии чата. Неизменяем после создания.
     */
    @NotNull
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    /**
     * Роль отправителя сообщения. Неизменяема после создания.
     *
     * @see MessageRole
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, updatable = false)
    private MessageRole role;

    /**
     * Текстовое содержимое сообщения. Неизменяемо после создания.
     */
    @NotNull
    @Lob
    @Column(name = "content", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Временная метка создания сообщения. Неизменяема после создания.
     */
    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
