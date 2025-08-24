package com.example.ragollama.monitoring;

import com.example.ragollama.monitoring.model.RagAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link RagAuditLog}.
 * Предоставляет стандартные CRUD-операции для работы с таблицей аудита.
 */
@Repository
public interface RagAuditLogRepository extends JpaRepository<RagAuditLog, UUID> {
}
