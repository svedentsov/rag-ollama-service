package com.example.ragollama.monitoring.domain;

import com.example.ragollama.monitoring.model.TrainingDataPair;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Реактивный репозиторий для TrainingDataPair.
 */
@Repository
public interface TrainingDataRepository extends ReactiveCrudRepository<TrainingDataPair, UUID> {
}
