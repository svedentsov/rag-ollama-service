package com.example.ragollama.evaluation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "evaluation_history")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "f1_score")
    private double f1Score;

    @Column(name = "recall")
    private double recall;

    @Column(name = "precision")
    private double precision;

    @Column(name = "mrr")
    private double meanReciprocalRank;

    @Column(name = "ndcg_at_5")
    private double ndcgAt5;

    @Column(name = "total_records")
    private int totalRecords;

    @Column(name = "triggering_source_id")
    private String triggeringSourceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
