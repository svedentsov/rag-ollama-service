package com.example.ragollama.agent.finops.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность для хранения логов использования LLM.
 */
@Table("llm_usage_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LlmUsageLog {

    @Id
    private UUID id;

    @NotBlank
    @Column( "username")
    private String username;

    @NotBlank
    @Column("model_name")
    private String modelName;

    @NotNull
    @Column("prompt_tokens")
    private long promptTokens;

    @NotNull
    @Column("completion_tokens")
    private long completionTokens;

    @NotNull
    @Column("total_tokens")
    private long totalTokens;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;
}
