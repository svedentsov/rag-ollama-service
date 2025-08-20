package com.example.ragollama.evaluation;

import com.example.ragollama.evaluation.model.EvaluationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Кастомный эндпоинт Spring Boot Actuator для запуска и просмотра
 * результатов оценки RAG-системы.
 * <p>
 * Доступен по пути {@code /actuator/rageval}.
 */
@Component
@Endpoint(id = "rageval")
@RequiredArgsConstructor
public class RagEvaluationEndpoint {

    private final RagEvaluationService evaluationService;

    /**
     * Запускает процесс оценки и возвращает результаты.
     *
     * @return {@link Mono} с объектом {@link EvaluationResult}.
     */
    @ReadOperation
    public Mono<EvaluationResult> evaluate() {
        return evaluationService.evaluate();
    }
}
