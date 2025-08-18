package com.example.ragollama.ingestion.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность JPA, представляющая задачу на обработку (индексацию) документа.
 * Каждая запись в таблице {@code document_jobs} соответствует одному запросу
 * на загрузку документа и отслеживает его состояние в процессе фоновой обработки.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"textContent", "errorMessage"}) // Исключаем большие текстовые поля
@EqualsAndHashCode(of = "id") // Реализуем equals/hashCode только по ID
@Entity
@Table(name = "document_jobs")
public class DocumentJob {

    /**
     * Уникальный идентификатор задачи. Является основой для equals и hashCode.
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
    @Lob
    @Column(name = "text_content", nullable = false, columnDefinition = "TEXT")
    private String textContent;

    /**
     * Сообщение об ошибке, если задача завершилась со статусом FAILED.
     */
    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Временная метка создания задачи.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Временная метка последнего обновления задачи.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Переводит задачу в статус COMPLETED.
     */
    public void markAsCompleted() {
        this.status = JobStatus.COMPLETED;
        this.errorMessage = null;
    }

    /**
     * Переводит задачу в статус FAILED и сохраняет сообщение об ошибке.
     * @param reason Причина сбоя.
     */
    public void markAsFailed(String reason) {
        this.status = JobStatus.FAILED;
        this.errorMessage = reason;
    }
}
