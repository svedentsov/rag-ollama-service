package com.example.ragollama.chat.domain.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.HashMap;
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
@Table("chat_sessions")
public class ChatSession {

    @Id
    @Column("session_id")
    private UUID sessionId;

    @Column("user_name")
    private String userName;

    @Column("chat_name")
    private String chatName;

    @Column("active_branches")
    @Builder.Default
    private Map<String, Object> activeBranches = new HashMap<>();

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private OffsetDateTime updatedAt;

    @Transient
    private ChatMessage lastMessage;
}
