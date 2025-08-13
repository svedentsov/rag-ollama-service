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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "session_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID sessionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, updatable = false)
    private MessageRole role;

    @NotNull
    @Lob
    @Column(name = "content", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String content;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
