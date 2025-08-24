package com.example.ragollama.qaagent.dynamic;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private int currentStepIndex;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Status {
        RUNNING, PENDING_APPROVAL, RESUMED_AFTER_APPROVAL, COMPLETED, FAILED
    }
}
