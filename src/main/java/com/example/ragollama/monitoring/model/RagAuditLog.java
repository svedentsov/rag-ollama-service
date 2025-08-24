package com.example.ragollama.monitoring.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Сущность JPA, представляющая одну запись в журнале аудита RAG-взаимодействий.
 * <p>
 * Каждая запись в таблице {@code rag_audit_log} является полным "слепком"
 * одного RAG-запроса, содержащим все данные, необходимые для анализа и отладки.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"originalQuery", "finalPrompt", "llmAnswer"}) // Исключаем большие текстовые поля
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "rag_audit_log")
public class RagAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "session_id")
    private UUID sessionId;

    private String username;

    @NotNull
    @Lob
    @Column(name = "original_query", nullable = false, columnDefinition = "TEXT")
    private String originalQuery;

    /**
     * Список имен документов-источников, хранится в формате JSONB для эффективности.
     */
    @Type(JsonType.class)
    @Column(name = "context_documents", columnDefinition = "jsonb")
    private List<String> contextDocuments;

    @Lob
    @Column(name = "final_prompt", columnDefinition = "TEXT")
    private String finalPrompt;

    @Lob
    @Column(name = "llm_answer", columnDefinition = "TEXT")
    private String llmAnswer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
