package com.example.ragollama.ingestion.domain.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Сущность JPA, представляющая задачу на обработку (индексацию) документа.
 * <p>
 * Эта версия включает поле `metadata` для хранения структурированных данных
 * и инкапсулирует бизнес-логику управления своим жизненным циклом
 * через методы {@code markAsCompleted()} и {@code markAsFailed()}.
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
     * Поле для хранения произвольных метаданных в формате JSON.
     */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

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
     * <p>
     * Сбрасывает сообщение об ошибке, обеспечивая чистое состояние
     * при успешном завершении.
     */
    public void markAsCompleted() {
        this.status = JobStatus.COMPLETED;
        this.errorMessage = null;
    }

    /**
     * Переводит задачу в статус FAILED и сохраняет сообщение об ошибке.
     *
     * @param reason Причина сбоя.
     */
    public void markAsFailed(String reason) {
        this.status = JobStatus.FAILED;
        this.errorMessage = reason;
    }
}
