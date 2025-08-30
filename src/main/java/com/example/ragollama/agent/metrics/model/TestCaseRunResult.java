package com.example.ragollama.agent.metrics.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность JPA, представляющая результат выполнения одного тест-кейса в рамках
 * одного тестового прогона.
 * <p>
 * Хранение этих гранулярных данных является основой для построения любой
 * глубокой аналитики по стабильности и производительности тестов.
 */
@Entity
@Table(name = "test_case_run_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseRunResult {
    /**
     * Уникальный идентификатор записи.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String projectId;

    /**
     * Ссылка на общий тестовый прогон, к которому относится этот результат.
     * Используется для связи с метаданными (коммит, ветка).
     * `ON DELETE CASCADE` гарантирует, что при удалении записи о прогоне
     * все связанные с ней результаты также будут удалены.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    private TestRunMetric testRun;

    /**
     * Полное имя класса, в котором находится тест.
     */
    private String className;

    /**
     * Имя тестового метода.
     */
    private String testName;

    /**
     * Статус выполнения теста (PASSED, FAILED, SKIPPED).
     */
    @Enumerated(EnumType.STRING)
    private TestResult.Status status;

    /**
     * Детальная информация об ошибке (стек-трейс), если тест упал.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String failureDetails;

    private long durationMs;

    /**
     * Временная метка создания записи.
     */
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
