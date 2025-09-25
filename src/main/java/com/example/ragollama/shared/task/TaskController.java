package com.example.ragollama.shared.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Task Management API", description = "API для управления и отслеживания асинхронных задач")
public class TaskController {

    private final CancellableTaskService taskService;

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Отменить выполнение задачи")
    public ResponseEntity<Void> cancelTask(@PathVariable UUID taskId) {
        boolean taskExists = taskService.cancel(taskId);
        return taskExists
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{taskId}/result")
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

    @ExceptionHandler(CancellationException.class)
    public ResponseEntity<String> handleCancellationException(CancellationException ex) {
        log.warn("Обработано исключение CancellationException: задача была отменена во время обработки запроса.");
        return ResponseEntity.ok("Задача была успешно отменена.");
    }
}
