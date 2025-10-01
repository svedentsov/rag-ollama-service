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

    @Id
    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "chat_name", nullable = false)
    private String chatName;

    /**
     * Связь One-to-Many с сообщениями.
     * orphanRemoval=true убран, каскадное удаление делегировано БД.
     * CascadeType.REMOVE убран.
     */
    @OneToMany(
            mappedBy = "session",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH},
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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
