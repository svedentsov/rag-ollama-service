package com.example.ragollama.agent.metrics.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность TestCaseRunResult, адаптированная для R2DBC.
 */
@Table("test_case_run_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseRunResult {
    @Id
    private UUID id;

    @Column("project_id")
    private String projectId;

    @Column("test_run_id")
    private UUID testRunId;

    @Column("class_name")
    private String className;

    @Column("test_name")
    private String testName;

    @Column("status")
    private TestResult.Status status;

    @Column("failure_details")
    private String failureDetails;

    @Column("duration_ms")
    private long durationMs;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;
}
