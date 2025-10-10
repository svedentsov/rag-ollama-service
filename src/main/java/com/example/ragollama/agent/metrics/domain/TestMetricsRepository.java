package com.example.ragollama.agent.metrics.domain;

import com.example.ragollama.agent.metrics.model.TestRunMetric;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Реактивный репозиторий для TestRunMetric.
 */
@Repository
public interface TestMetricsRepository extends ReactiveCrudRepository<TestRunMetric, UUID> {
    Flux<TestRunMetric> findByRunTimestampAfterOrderByRunTimestampAsc(OffsetDateTime since);
}
