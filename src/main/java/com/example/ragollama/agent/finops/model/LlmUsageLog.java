package com.example.ragollama.agent.finops.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность JPA для логирования каждого вызова LLM.
 * Является источником данных для FinOps-аналитики и контроля квот.
 */
@Entity
@Table(name = "llm_usage_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LlmUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false, updatable = false)
    private String username;

    @NotBlank
    @Column(nullable = false, updatable = false)
    private String modelName;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Long promptTokens;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Long completionTokens;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Long totalTokens;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
