package com.example.ragollama.shared.task;

import com.example.ragollama.orchestration.dto.UniversalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер, предоставляющий REST API для управления жизненным циклом асинхронных задач.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Task Management API", description = "API для управления и отслеживания асинхронных задач")
public class TaskController {

    private final CancellableTaskService taskService;
    private final TaskStateService taskStateService;

    /**
     * Отменяет выполнение асинхронной задачи.
     *
     * @param taskId ID задачи для отмены.
     * @return {@link ResponseEntity} со статусом 200 OK или 404 Not Found.
     */
    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "Отменить выполнение задачи")
    public ResponseEntity<Void> cancelTask(@PathVariable UUID taskId) {
        boolean taskExists = taskService.cancel(taskId);
        return taskExists
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * Возвращает текущий статус асинхронной задачи.
     *
     * @param taskId ID задачи.
     * @return {@link ResponseEntity} со статусом задачи или 404 Not Found.
     */
    @GetMapping("/tasks/{taskId}/status")
    @Operation(summary = "Получить статус асинхронной задачи")
    public ResponseEntity<Map<String, String>> getTaskStatus(@PathVariable UUID taskId) {
        return taskService.getStatus(taskId)
                .map(status -> ResponseEntity.ok(Map.of("status", status.name())))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Находит ID и статус активной задачи, связанной с указанной сессией чата.
     *
     * @param sessionId ID сессии чата.
     * @return {@link ResponseEntity} с информацией о задаче или 404 Not Found.
     */
    @GetMapping("/sessions/{sessionId}/active-task")
    @Operation(summary = "Получить активную задачу для сессии")
    public ResponseEntity<Map<String, Object>> getActiveTaskForSession(@PathVariable UUID sessionId) {
        return taskStateService.getActiveTaskIdForSession(sessionId)
                .flatMap(taskId -> taskService.getStatus(taskId)
                        .map(status -> {
                            Map<String, Object> body = new HashMap<>();
                            body.put("taskId", taskId);
                            body.put("status", status.name());
                            return body;
                        }))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Позволяет клиенту переподключиться к потоку событий (SSE) уже запущенной задачи.
     *
     * @param taskId ID задачи.
     * @return {@link Flux} с потоком событий.
     * @throws TaskNotFoundException если задача не найдена или уже завершена.
     */
    @GetMapping(value = "/tasks/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Переподключиться к потоку событий задачи")
    public Flux<UniversalResponse> reconnectToTaskStream(@PathVariable UUID taskId) {
        log.info("Клиент запрашивает переподключение к потоку задачи {}", taskId);
        return taskService.getTaskStream(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Поток для задачи " + taskId + " не найден или уже завершен."));
    }

    /**
     * Ожидает завершения асинхронной задачи и возвращает ее полный результат.
     *
     * @param taskId ID задачи.
     * @return {@link DeferredResult} для асинхронной обработки HTTP-запроса.
     * @throws TaskNotFoundException если задача не найдена.
     */
    @GetMapping("/tasks/{taskId}/result")
    @Operation(summary = "Получить результат выполнения задачи")
    public DeferredResult<ResponseEntity<?>> getTaskResult(@PathVariable UUID taskId) {
        CompletableFuture<?> taskFuture = taskService.getTask(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Задача с ID " + taskId + " не найдена или уже завершена."));

        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(30_000L,
                ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("Таймаут ожидания результата задачи."));

        taskFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof CancellationException || throwable.getCause() instanceof CancellationException) {
                    deferredResult.setResult(ResponseEntity.ok("Задача была отменена."));
                } else {
                    deferredResult.setErrorResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Задача завершилась с ошибкой: " + throwable.getMessage()));
                }
            } else {
                deferredResult.setResult(ResponseEntity.ok(result));
            }
        });

        return deferredResult;
    }

    /**
     * Обработчик для {@link CancellationException}, чтобы возвращать корректный ответ
     * вместо стандартной ошибки сервера.
     *
     * @param ex Исключение.
     * @return {@link ResponseEntity} со статусом 200 OK и сообщением об отмене.
     */
    @ExceptionHandler(CancellationException.class)
    public ResponseEntity<String> handleCancellationException(CancellationException ex) {
        log.warn("Обработано исключение CancellationException: задача была отменена во время обработки запроса.");
        return ResponseEntity.ok("Задача была успешно отменена.");
    }
}
