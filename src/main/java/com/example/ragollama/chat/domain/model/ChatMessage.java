package com.example.ragollama.chat.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность JPA, представляющая одно сообщение в диалоге.
 * <p>
 * В этой версии поле `sessionId` заменено на полноценную связь `@ManyToOne`
 * с сущностью `ChatSession`, что обеспечивает целостность данных на уровне ORM.
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

    /**
     * Связь "многие-к-одному" с родительской сессией.
     * `JoinColumn` указывает на колонку `session_id`, которая будет внешним ключом.
     * `nullable = false` гарантирует, что сообщение не может существовать без сессии.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Column(name = "parent_id", updatable = false)
    private UUID parentId;

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
     * Вспомогательный метод для получения ID сессии без загрузки всей сущности.
     *
     * @return UUID сессии.
     */
    public UUID getSessionId() {
        return (session != null) ? session.getSessionId() : null;
    }
}
