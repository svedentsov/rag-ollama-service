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
 * <p>
 * В этой версии связь с сообщениями настроена так, чтобы делегировать
 * каскадное удаление на уровень базы данных, устраняя StaleObjectStateException.
 * Hibernate по-прежнему управляет созданием и обновлением сообщений при сохранении сессии.
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
     * CascadeType.ALL заменен на более гранулярный набор, исключающий REMOVE.
     * orphanRemoval=true убран.
     * Теперь удаление сессии приведет к выполнению одного DELETE-запроса,
     * а PostgreSQL сам позаботится об удалении связанных сообщений благодаря
     * 'ON DELETE CASCADE' в миграции V21.
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
     *
     * @param message Сообщение для добавления.
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setSession(this);
    }
}
