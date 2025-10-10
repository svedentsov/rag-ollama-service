package com.example.ragollama.agent.finops.domain;

import com.example.ragollama.agent.finops.model.LlmUsageLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Реактивный репозиторий для {@link LlmUsageLog}, обеспечивающий доступ к данным
 * об использовании языковых моделей.
 */
@Repository
public interface LlmUsageLogRepository extends ReactiveCrudRepository<LlmUsageLog, UUID> {
}
