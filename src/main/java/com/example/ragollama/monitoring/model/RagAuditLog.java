package com.example.ragollama.monitoring.model;

import com.example.ragollama.rag.domain.model.SourceCitation;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Сущность RagAuditLog, адаптированная для R2DBC.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"originalQuery", "finalPrompt", "llmAnswer"})
@EqualsAndHashCode(of = "id")
@Table("rag_audit_log")
public class RagAuditLog {

    @Id
    private UUID id;

    @Column("request_id")
    private String requestId;

    @Column("session_id")
    private UUID sessionId;

    private String username;

    @NotNull
    @Column("original_query")
    private String originalQuery;

    @Column("source_citations")
    private List<SourceCitation> sourceCitations;

    @Column("final_prompt")
    private String finalPrompt;

    @Column("llm_answer")
    private String llmAnswer;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;
}
