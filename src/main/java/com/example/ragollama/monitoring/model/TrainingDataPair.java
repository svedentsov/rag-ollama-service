package com.example.ragollama.monitoring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность JPA, представляющая одну обучающую пару (запрос, документ)
 * для последующего дообучения моделей.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "training_data_pairs")
public class TrainingDataPair {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Lob
    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @NotNull
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @NotNull
    @Column(nullable = false)
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_feedback_id")
    private FeedbackLog sourceFeedback;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
