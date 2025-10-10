package com.example.ragollama.shared.task.model;

import com.example.ragollama.shared.task.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность AsyncTask, адаптированная для R2DBC.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("async_tasks")
public class AsyncTask {

    @Id
    private UUID id;

    @Column("session_id")
    private UUID sessionId;

    @NotNull
    @Column
    private TaskStatus status;

    @Column("error_message")
    private String errorMessage;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private OffsetDateTime updatedAt;

    public void markAsFailed(String message) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = message;
    }
}
