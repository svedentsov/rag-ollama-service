package com.example.ragollama.evaluation.domain;

import com.example.ragollama.evaluation.model.EvaluationHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Реактивный репозиторий для EvaluationHistory.
 */
@Repository
public interface EvaluationHistoryRepository extends ReactiveCrudRepository<EvaluationHistory, UUID> {

    /**
     * Находит последние записи истории.
     *
     * @param pageable Сортировка и ограничение.
     * @return Поток записей.
     */
    Flux<EvaluationHistory> findAllBy(Pageable pageable);
}
