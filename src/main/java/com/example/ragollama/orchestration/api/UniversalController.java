package com.example.ragollama.orchestration.api;

import com.example.ragollama.orchestration.OrchestrationService;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.shared.task.TaskSubmissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Универсальный контроллер, являющийся единой точкой входа для всех
 * интерактивных запросов к AI-системе.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orchestrator")
@RequiredArgsConstructor
@Tag(name = "Universal AI Orchestrator", description = "Единый API для взаимодействия с системой (RAG, Chat, Agents)")
public class UniversalController {

    private final OrchestrationService orchestrationService;

    /**
     * Асинхронно запускает выполнение задачи и немедленно возвращает ее ID.
     *
     * @param request Унифицированный DTO с запросом от пользователя.
     * @return {@link ResponseEntity} со статусом 202 (Accepted) и {@link TaskSubmissionResponse} в теле.
     */
    @PostMapping("/ask")
    @Operation(summary = "Универсальный асинхронный эндпоинт для запуска задач",
            description = "Немедленно принимает задачу в обработку и возвращает ее ID для отслеживания.")
    @ApiResponse(responseCode = "202", description = "Задача принята в обработку")
    public ResponseEntity<TaskSubmissionResponse> ask(@Valid @RequestBody UniversalRequest request) {
        TaskSubmissionResponse response = orchestrationService.processAsync(request);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    /**
     * Обрабатывает любой пользовательский запрос в потоковом режиме (Server-Sent Events).
     *
     * @param request Унифицированный DTO с запросом от пользователя.
     * @return Реактивный поток UniversalResponse. Spring автоматически преобразует каждый
     *         элемент в строку "data: {...}\n\n".
     */
    @PostMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Универсальный потоковый эндпоинт (SSE)")
    public Flux<UniversalResponse> askStream(@Valid @RequestBody UniversalRequest request) {
        return orchestrationService.processStream(request);
    }
}
