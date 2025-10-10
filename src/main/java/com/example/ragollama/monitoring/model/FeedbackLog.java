package com.example.ragollama.monitoring.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность FeedbackLog, адаптированная для R2DBC.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "id")
@Table("feedback_log")
public class FeedbackLog {

    @Id
    private UUID id;

    @NotNull
    @Column("request_id")
    private String requestId;

    @NotNull
    @Column("is_helpful")
    private Boolean isHelpful;

    @Column("user_comment")
    private String userComment;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;
}
