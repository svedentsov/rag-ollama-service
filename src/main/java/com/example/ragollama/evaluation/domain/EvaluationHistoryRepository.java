package com.example.ragollama.evaluation.domain;

import com.example.ragollama.evaluation.model.EvaluationHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationHistoryRepository extends JpaRepository<EvaluationHistory, UUID> {

    /**
     * Находит последний успешный результат оценки.
     *
     * @param pageable Сортировка и ограничение (должно быть PageRequest.of(0, 1)).
     * @return Список, содержащий не более одного (самого последнего) результата.
     */
    List<EvaluationHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
