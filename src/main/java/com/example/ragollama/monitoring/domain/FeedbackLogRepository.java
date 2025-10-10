package com.example.ragollama.monitoring.domain;

import com.example.ragollama.monitoring.model.FeedbackLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Реактивный репозиторий для FeedbackLog.
 */
@Repository
public interface FeedbackLogRepository extends ReactiveCrudRepository<FeedbackLog, UUID> {
}
