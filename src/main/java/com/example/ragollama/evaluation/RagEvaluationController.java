package com.example.ragollama.evaluation;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.evaluation.api.dto.FeedbackToTestRequest;
import com.example.ragollama.evaluation.model.EvaluationResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер и Actuator-эндпоинт для управления оценкой RAG-системы.
 * <p>
 * Этот класс объединяет две ответственности:
 * 1. Предоставляет кастомный эндпоинт Spring Boot Actuator для запуска
 *    оценки RAG-системы по пути {@code /actuator/rageval}.
 * 2. Предоставляет стандартный REST API для запуска агента, который
 *    преобразует пользовательский фидбэк в новый тест.
 */
@Component
@RestController
@RequestMapping("/api/v1/evaluation")
@Endpoint(id = "rageval")
@RequiredArgsConstructor
public class RagEvaluationController {

    private final RagEvaluationService evaluationService;
    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает процесс оценки через Actuator и возвращает результаты.
     * <p>
     * Доступен по пути {@code GET /actuator/rageval}.
     *
     * @return {@link Mono} с объектом {@link EvaluationResult}.
     */
    @ReadOperation
    public Mono<EvaluationResult> evaluate() {
        return evaluationService.evaluate();
    }

    /**
     * Запускает агента для анализа фидбэка и автоматического создания нового теста.
     * <p>
     * Доступен по пути {@code POST /api/v1/evaluation/generate-test-from-feedback}.
     *
     * @param request DTO с ID фидбэка.
     * @return {@link CompletableFuture} с результатом, содержащим новый `GoldenRecord`.
     */
    @PostMapping("/generate-test-from-feedback")
    public CompletableFuture<List<AgentResult>> generateTestFromFeedback(@Valid @RequestBody FeedbackToTestRequest request) {
        return orchestratorService.invokePipeline("feedback-to-test-pipeline", request.toAgentContext());
    }
}
