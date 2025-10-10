package com.example.ragollama.ingestion.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Сущность DocumentJob, адаптированная для R2DBC.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"textContent", "errorMessage"})
@EqualsAndHashCode(of = "id")
@Table("document_jobs")
public class DocumentJob {

    @Id
    private UUID id;

    @NotNull
    @Column("source_name")
    private String sourceName;

    @NotNull
    @Column("status")
    private JobStatus status;

    @NotNull
    @Column("text_content")
    private String textContent;

    @Column("metadata")
    private Map<String, Object> metadata;

    @Column("error_message")
    private String errorMessage;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private OffsetDateTime updatedAt;

    public void markAsCompleted() {
        this.status = JobStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void markAsFailed(String reason) {
        this.status = JobStatus.FAILED;
        this.errorMessage = reason;
    }
}
