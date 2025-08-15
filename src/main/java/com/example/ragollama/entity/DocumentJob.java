package com.example.ragollama.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность JPA, представляющая задачу на обработку (индексацию) документа.
 * <p>
 * Каждая запись в таблице {@code document_jobs} соответствует одному запросу
 * на загрузку документа и отслеживает его состояние в процессе фоновой обработки.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_jobs")
public class DocumentJob {

    /**
     * Уникальный идентификатор задачи (Job ID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Имя источника документа.
     */
    @NotNull
    @Column(name = "source_name", nullable = false)
    private String sourceName;

    /**
     * Текущий статус задачи.
     * @see JobStatus
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    /**
     * Полное текстовое содержимое документа, ожидающего обработки.
     */
    @NotNull
    @Lob // Large Object, указывает, что поле может хранить большой объем текста.
    @Column(name = "text_content", nullable = false, columnDefinition = "TEXT")
    private String textContent;

    /**
     * Сообщение об ошибке, если задача завершилась со статусом FAILED.
     */
    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Временная метка создания задачи. Автоматически устанавливается Hibernate.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Временная метка последнего обновления задачи. Автоматически обновляется Hibernate.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}