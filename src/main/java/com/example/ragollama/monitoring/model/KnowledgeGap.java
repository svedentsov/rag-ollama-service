package com.example.ragollama.monitoring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность JPA, представляющая один "пробел в знаниях".
 * <p>
 * Каждая запись в таблице {@code knowledge_gaps} соответствует одному
 * запросу пользователя, на который система не смогла найти релевантную
 * информацию в своей базе знаний.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "knowledge_gaps")
public class KnowledgeGap {

    /**
     * Уникальный идентификатор записи.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Текст запроса пользователя, который не дал результатов.
     */
    @NotNull
    @Lob
    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    /**
     * Временная метка обнаружения пробела в знаниях.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
