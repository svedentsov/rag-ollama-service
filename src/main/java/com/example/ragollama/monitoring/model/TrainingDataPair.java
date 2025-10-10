package com.example.ragollama.monitoring.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность TrainingDataPair, адаптированная для R2DBC.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "id")
@Table("training_data_pairs")
public class TrainingDataPair {

    @Id
    private UUID id;

    @NotNull
    @Column("query_text")
    private String queryText;

    @NotNull
    @Column("document_id")
    private UUID documentId;

    @NotNull
    @Column
    private String label;

    @Column("source_feedback_id")
    private UUID sourceFeedbackId;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;
}
