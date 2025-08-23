package com.example.ragollama.monitoring;

import com.example.ragollama.monitoring.model.KnowledgeGap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link KnowledgeGap}.
 * Предоставляет стандартные CRUD-операции для работы с таблицей,
 * хранящей информацию о пробелах в знаниях.
 */
@Repository
public interface KnowledgeGapRepository extends JpaRepository<KnowledgeGap, UUID> {
}
