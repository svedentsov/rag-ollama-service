package com.example.ragollama.chat.domain.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Сущность JPA, представляющая одну сессию чата.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    /**
     * Уникальный идентификатор сессии.
     * Теперь генерируется автоматически на уровне JPA.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id")
    private UUID sessionId;

    /**
     * Имя пользователя, которому принадлежит сессия.
     */
    @Column(name = "user_name", nullable = false)
    private String userName;

    /**
     * Название чата, видимое пользователю.
     */
    @Column(name = "chat_name", nullable = false)
    private String chatName;

    /**
     * Хранит выбор активных веток для каждого родительского сообщения.
     * Ключ - parent_message_id, Значение - active_child_message_id.
     * Хранится в формате JSONB для гибкости.
     */
    @Type(JsonType.class)
    @Column(name = "active_branches", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> activeBranches = new HashMap<>();

    @OneToMany(
            mappedBy = "session",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH},
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * Временная метка создания сессии.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Временная метка последнего обновления сессии.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Транзиентное поле для хранения последнего сообщения (не сохраняется в БД).
     */
    @Transient
    private ChatMessage lastMessage;

    /**
     * Helper-метод для безопасного добавления сообщения в сессию.
     * Гарантирует, что обе стороны двунаправленной связи синхронизированы.
     * @param message Сообщение для добавления.
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setSession(this);
    }
}
