package com.example.ragollama.agent.dynamic;

import com.example.ragollama.agent.AgentResult;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Сущность JPA, представляющая полное состояние выполнения одного динамического конвейера.
 * <p>
 * Хранит в себе не только план и текущий контекст, но и историю уже
 * выполненных шагов, что критически важно для возобновления и отладки.
 */
@Entity
@Table(name = "pipeline_executions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<PlanStep> planSteps;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> accumulatedContext;

    @Builder.Default
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<AgentResult> executionHistory = new ArrayList<>();

    private int currentStepIndex;

    @Builder.Default
    private boolean resumedAfterApproval = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Перечисление возможных статусов выполнения конвейера.
     */
    public enum Status {
        RUNNING, PENDING_APPROVAL, COMPLETED, FAILED
    }
}
