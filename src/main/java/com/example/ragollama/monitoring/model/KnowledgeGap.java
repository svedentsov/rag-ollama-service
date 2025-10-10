package com.example.ragollama.monitoring.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность KnowledgeGap, адаптированная для R2DBC.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("knowledge_gaps")
public class KnowledgeGap {

    @Id
    private UUID id;

    @NotNull
    @Column("query_text")
    private String queryText;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;
}
