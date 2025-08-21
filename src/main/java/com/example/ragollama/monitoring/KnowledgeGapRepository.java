package com.example.ragollama.monitoring;

import com.example.ragollama.monitoring.model.KnowledgeGap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KnowledgeGapRepository extends JpaRepository<KnowledgeGap, UUID> {
}
