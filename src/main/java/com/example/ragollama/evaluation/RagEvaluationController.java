package com.example.ragollama.evaluation;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.evaluation.api.dto.FeedbackToTestRequest;
import com.example.ragollama.evaluation.model.EvaluationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Контроллер и Actuator-эндпоинт для управления оценкой RAG-системы.
 */
@Component
@RestController
@RequestMapping("/api/v1/evaluation")
@Endpoint(id = "rageval")
@RequiredArgsConstructor
@Tag(name = "RAG Evaluation API", description = "API для оценки качества RAG и работы с фидбэком")
public class RagEvaluationController {

    private final RagEvaluationService evaluationService;
    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает процесс оценки через Actuator и возвращает результаты.
     *
     * @return {@link Mono} с объектом {@link EvaluationResult}.
     */
    @ReadOperation
    @Operation(summary = "Запустить оценку RAG по 'золотому датасету' (Actuator)")
    public Mono<EvaluationResult> evaluate() {
        return evaluationService.evaluate();
    }

    /**
     * Запускает конвейер для анализа фидбэка и автоматического создания нового теста.
     *
     * @param request DTO с ID фидбэка для анализа.
     * @return {@link Mono} с результатом, содержащим информацию о созданном тесте.
     */
    @PostMapping("/generate-test-from-feedback")
    @Operation(summary = "Сгенерировать тест для 'золотого датасета' из фидбэка пользователя")
    public Mono<List<AgentResult>> generateTestFromFeedback(@Valid @RequestBody FeedbackToTestRequest request) {
        return orchestratorService.invoke("feedback-to-test-pipeline", request.toAgentContext());
    }
}
