package com.example.ragollama.chat.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность JPA, представляющая одно сообщение в диалоге.
 * Добавлены поля parentId и taskId для поддержки ветвления и асинхронных операций.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"content", "session"})
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    /**
     * ID родительского сообщения. Для сообщения пользователя это null.
     * Для ответа ассистента это ID сообщения пользователя.
     * Позволяет создавать древовидные структуры и ветвление.
     */
    @Column(name = "parent_id", updatable = false)
    private UUID parentId;

    /**
     * ID асинхронной задачи, которая сгенерировала это сообщение.
     * Позволяет связать ответ с операцией для сбора фидбэка.
     */
    @Column(name = "task_id")
    private UUID taskId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, updatable = false)
    private MessageRole role;

    @NotNull
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Вспомогательный метод для получения ID сессии без загрузки ленивой сущности.
     * @return ID сессии.
     */
    public UUID getSessionId() {
        return (session != null) ? session.getSessionId() : null;
    }
}
