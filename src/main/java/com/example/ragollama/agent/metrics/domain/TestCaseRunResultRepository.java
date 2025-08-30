package com.example.ragollama.agent.metrics.domain;

import com.example.ragollama.agent.metrics.model.TestCaseRunResult;
import com.example.ragollama.agent.testanalysis.model.FlakyTestInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link TestCaseRunResult}.
 * <p>
 * Содержит сложный JPQL-запрос для выполнения аналитической агрегации
 * и выявления нестабильных тестов.
 */
@Repository
public interface TestCaseRunResultRepository extends JpaRepository<TestCaseRunResult, UUID> {

    /**
     * Находит и агрегирует информацию о нестабильных тестах за указанный период.
     * <p>
     * Этот JPQL-запрос выполняет следующие действия:
     * 1. Фильтрует запуски тестов по дате (`:since`).
     * 2. Группирует результаты по уникальному имени теста (`className`, `testName`).
     * 3. Для каждой группы вычисляет:
     * - Общее количество запусков (`COUNT`).
     * - Количество падений (`SUM` по условию).
     * - Процент падений (Flakiness Rate).
     * 4. Фильтрует группы (`HAVING`), оставляя только те, которые:
     * - Запускались не менее `:minRuns` раз.
     * - Имеют процент падений выше порога `:threshold`.
     * 5. Сортирует результат по убыванию процента падений.
     * <p>
     * Использование конструктора `new` с полным именем класса делает запрос
     * устойчивым к рефакторингу.
     *
     * @param since     Начальная дата для анализа.
     * @param threshold Минимальный процент падений (в долях, например, 0.05 для 5%).
     * @param minRuns   Минимальное количество запусков для включения в анализ.
     * @return Список DTO {@link FlakyTestInfo} с информацией о нестабильных тестах.
     */
    @Query(value = """
            SELECT new com.example.ragollama.agent.testanalysis.model.FlakyTestInfo(
                t.className,
                t.testName,
                COUNT(t.id),
                SUM(CASE WHEN t.status = 'FAILED' THEN 1 ELSE 0 END),
                (SUM(CASE WHEN t.status = 'FAILED' THEN 1.0 ELSE 0.0 END) / COUNT(t.id)) * 100.0
            )
            FROM TestCaseRunResult t
            WHERE t.createdAt >= :since
            GROUP BY t.className, t.testName
            HAVING COUNT(t.id) >= :minRuns
               AND (SUM(CASE WHEN t.status = 'FAILED' THEN 1.0 ELSE 0.0 END) / COUNT(t.id)) > :threshold
            ORDER BY (SUM(CASE WHEN t.status = 'FAILED' THEN 1.0 ELSE 0.0 END) / COUNT(t.id)) DESC
            """)
    List<FlakyTestInfo> findFlakyTests(
            @Param("since") OffsetDateTime since,
            @Param("threshold") double threshold,
            @Param("minRuns") long minRuns);
}
