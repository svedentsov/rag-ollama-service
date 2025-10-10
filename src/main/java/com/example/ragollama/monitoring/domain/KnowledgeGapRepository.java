package com.example.ragollama.monitoring.domain;

import com.example.ragollama.monitoring.model.KnowledgeGap;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Реактивный репозиторий для KnowledgeGap.
 */
@Repository
public interface KnowledgeGapRepository extends ReactiveCrudRepository<KnowledgeGap, UUID> {
}
