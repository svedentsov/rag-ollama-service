package com.example.ragollama.shared.task.model;

import com.example.ragollama.shared.task.TaskStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность JPA для персистентного хранения состояния асинхронной задачи.
 * Является источником истины о жизненном цикле фоновых операций.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "async_tasks")
public class AsyncTask {

    /**
     * Уникальный идентификатор задачи.
     */
    @Id
    private UUID id;

    /**
     * ID сессии чата, инициировавшей задачу. Может быть null.
     */
    @Column(name = "session_id")
    private UUID sessionId;

    /**
     * Текущий статус выполнения задачи.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    /**
     * Сообщение об ошибке, если задача завершилась неудачно.
     */
    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Временная метка создания задачи.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Временная метка последнего обновления статуса задачи.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Бизнес-метод для перевода задачи в статус FAILED.
     * @param message Сообщение об ошибке.
     */
    public void markAsFailed(String message) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = message;
    }
}
