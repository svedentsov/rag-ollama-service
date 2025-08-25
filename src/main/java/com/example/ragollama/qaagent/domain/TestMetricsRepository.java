package com.example.ragollama.qaagent.domain;

import com.example.ragollama.qaagent.model.TestRunMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link TestRunMetric}.
 */
@Repository
public interface TestMetricsRepository extends JpaRepository<TestRunMetric, UUID> {
    /**
     * Находит все записи о тестовых прогонах, выполненных после указанной
     * временной метки, и сортирует их в хронологическом порядке.
     *
     * @param since Начальная дата для выборки.
     * @return Список {@link TestRunMetric}.
     */
    List<TestRunMetric> findByRunTimestampAfterOrderByRunTimestampAsc(OffsetDateTime since);
}
