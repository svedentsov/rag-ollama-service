package com.example.ragollama.chat.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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
     * Связь One-to-Many с сообщениями.
     * `cascade = CascadeType.ALL` и `orphanRemoval = true` обеспечивают полный
     * контроль жизненного цикла сообщений через сессию.
     */
    @OneToMany(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
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
