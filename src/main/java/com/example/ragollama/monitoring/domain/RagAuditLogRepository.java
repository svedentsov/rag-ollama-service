package com.example.ragollama.monitoring.domain;

import com.example.ragollama.monitoring.model.RagAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link RagAuditLog}.
 */
@Repository
public interface RagAuditLogRepository extends JpaRepository<RagAuditLog, UUID> {
    /**
     * Проверяет существование записи по `requestId`.
     *
     * @param requestId Идентификатор запроса.
     * @return {@code true}, если запись существует.
     */
    boolean existsByRequestId(String requestId);

    /**
     * Находит запись аудита по `requestId`.
     *
     * @param requestId Идентификатор запроса.
     * @return {@link Optional} с найденной записью.
     */
    Optional<RagAuditLog> findByRequestId(String requestId);
}
