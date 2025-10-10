package com.example.ragollama.monitoring.domain;

import com.example.ragollama.monitoring.model.RagAuditLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Реактивный репозиторий для RagAuditLog.
 */
@Repository
public interface RagAuditLogRepository extends ReactiveCrudRepository<RagAuditLog, UUID> {
    Mono<Boolean> existsByRequestId(String requestId);
    Mono<RagAuditLog> findByRequestId(String requestId);
}
