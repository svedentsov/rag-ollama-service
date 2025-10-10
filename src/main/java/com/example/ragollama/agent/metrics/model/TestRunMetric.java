package com.example.ragollama.agent.metrics.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность TestRunMetric, адаптированная для R2DBC.
 */
@Table("test_run_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRunMetric {

    @Id
    private UUID id;

    @Column("commit_hash")
    private String commitHash;

    @Column("branch_name")
    private String branchName;

    @Column("total_count")
    private int totalCount;

    @Column("passed_count")
    private int passedCount;

    @Column("failed_count")
    private int failedCount;

    @Column("skipped_count")
    private int skippedCount;

    @Column("duration_ms")
    private long durationMs;

    @CreatedDate
    @Column("run_timestamp")
    private OffsetDateTime runTimestamp;

    @Column("project_id")
    private String projectId;
}
