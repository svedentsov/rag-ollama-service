package com.example.ragollama.monitoring.domain;

import com.example.ragollama.monitoring.model.FeedbackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link FeedbackLog}.
 */
@Repository
public interface FeedbackLogRepository extends JpaRepository<FeedbackLog, UUID> {
}
