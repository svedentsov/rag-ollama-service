package com.example.ragollama.agent.dynamic;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Репозиторий для управления сущностями ExecutionState с использованием R2DBC.
 * Заменяет JpaRepository на R2dbcRepository для неблокирующего доступа к данным.
 */
@Repository
public interface ExecutionStateRepository extends R2dbcRepository<ExecutionState, UUID> {
}
