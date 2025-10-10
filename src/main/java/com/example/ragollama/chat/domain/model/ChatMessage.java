package com.example.ragollama.chat.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность, представляющая одно сообщение в диалоге, адаптированная для R2DBC.
 * <p>
 * В эту версию добавлено поле `version` с аннотацией {@link Version} для реализации
 * механизма оптимистичной блокировки, что предотвращает потерянные обновления
 * при одновременном редактировании.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"content"})
@EqualsAndHashCode(of = "id")
@Table("chat_messages")
public class ChatMessage {

    @Id
    private UUID id;

    @Column("session_id")
    private UUID sessionId;

    @Column("parent_id")
    private UUID parentId;

    @Column("task_id")
    private UUID taskId;

    @NotNull
    @Column("role")
    private MessageRole role;

    @NotNull
    @Column("content")
    private String content;

    @NotNull
    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;

    /**
     * Поле для реализации оптимистичной блокировки.
     * Spring Data R2DBC автоматически управляет этим полем:
     * инкрементирует при каждом UPDATE и проверяет при сохранении.
     */
    @Version
    private Long version;
}
