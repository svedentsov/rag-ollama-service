package com.example.ragollama.qaagent.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность JPA, представляющая агрегированные метрики одного тестового прогона.
 * <p>
 * Каждая запись в таблице {@code test_run_metrics} является "слепком"
 * одного запуска тестов в CI/CD, содержащим ключевые показатели для
 * последующего анализа.
 */
@Entity
@Table(name = "test_run_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRunMetric {

    /**
     * Уникальный идентификатор записи.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * SHA-хэш коммита, для которого был выполнен запуск.
     */
    private String commitHash;

    /**
     * Имя ветки, для которой был выполнен запуск.
     */
    private String branchName;

    /**
     * Общее количество выполненных тестов.
     */
    private int totalCount;

    /**
     * Количество успешно пройденных тестов.
     */
    private int passedCount;

    /**
     * Количество упавших тестов.
     */
    private int failedCount;

    /**
     * Количество пропущенных тестов.
     */
    private int skippedCount;

    /**
     * Общая продолжительность выполнения тестового набора в миллисекундах.
     */
    private long durationMs;

    /**
     * Временная метка завершения тестового прогона.
     */
    @Column(nullable = false, updatable = false)
    private OffsetDateTime runTimestamp;

    @Column(nullable = false)
    private String projectId;
}
