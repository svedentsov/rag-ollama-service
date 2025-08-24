package com.example.ragollama.monitoring.domain;

import com.example.ragollama.monitoring.model.TrainingDataPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link TrainingDataPair}.
 */
@Repository
public interface TrainingDataRepository extends JpaRepository<TrainingDataPair, UUID> {
}
