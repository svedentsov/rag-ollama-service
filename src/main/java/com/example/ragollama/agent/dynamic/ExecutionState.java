package com.example.ragollama.agent.dynamic;

import com.example.ragollama.agent.AgentResult;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Сущность, представляющая полное состояние выполнения одного динамического конвейера.
 * Адаптирована для работы со Spring Data R2DBC с использованием кастомных конвертеров.
 */
@Table("pipeline_executions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionState {

    @Id
    private UUID id;

    @Column("session_id")
    private UUID sessionId;

    @Column("status")
    private Status status;

    @Column("plan_steps")
    private List<PlanStep> planSteps;

    @Column("accumulated_context")
    private Map<String, Object> accumulatedContext;

    @Builder.Default
    @Column("execution_history")
    private List<AgentResult> executionHistory = new ArrayList<>();

    @Column("current_step_index")
    private int currentStepIndex;

    @Builder.Default
    @Column("resumed_after_approval")
    private boolean resumedAfterApproval = false;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private OffsetDateTime updatedAt;

    /**
     * Перечисление возможных статусов выполнения конвейера.
     */
    public enum Status {
        RUNNING, PENDING_APPROVAL, COMPLETED, FAILED
    }
}
