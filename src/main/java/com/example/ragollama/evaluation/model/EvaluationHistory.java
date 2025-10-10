package com.example.ragollama.evaluation.model;

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
 * Сущность для истории оценок, адаптированная для R2DBC.
 */
@Table("evaluation_history")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationHistory {

    @Id
    private UUID id;

    @Column("f1_score")
    private double f1Score;

    @Column("recall")
    private double recall;

    @Column("precision")
    private double precision;

    @Column("mrr")
    private double meanReciprocalRank;

    @Column("ndcg_at_5")
    private double ndcgAt5;

    @Column("total_records")
    private int totalRecords;

    @Column("triggering_source_id")
    private String triggeringSourceId;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;
}
