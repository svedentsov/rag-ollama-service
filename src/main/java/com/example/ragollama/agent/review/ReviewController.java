package com.example.ragollama.agent.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * API для управления процессом утверждения (Human-in-the-Loop).
 * <p>
 * Предоставляет внешним системам или UI возможность взаимодействовать
 * с приостановленными рабочими процессами, утверждая или отклоняя их.
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Review API", description = "API для утверждения или отклонения шагов конвейера")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Утверждает выполнение приостановленного конвейера.
     * <p>
     * После вызова этого эндпоинта, `DynamicPipelineExecutionService` асинхронно
     * возобновит выполнение плана с того шага, на котором он был остановлен.
     *
     * @param executionId ID конвейера, ожидающего утверждения.
     * @return {@link ResponseEntity} со статусом 202 (Accepted).
     */
    @PostMapping("/{executionId}/approve")
    @Operation(summary = "Утвердить выполнение приостановленного шага")
    @ApiResponse(responseCode = "202", description = "Запрос на возобновление принят")
    @ApiResponse(responseCode = "404", description = "Выполнение с таким ID не найдено")
    @ApiResponse(responseCode = "409", description = "Выполнение не ожидает утверждения")
    public ResponseEntity<Void> approveExecution(
            @Parameter(description = "ID выполнения, полученный от агента") @PathVariable UUID executionId) {
        reviewService.approve(executionId);
        return ResponseEntity.accepted().build();
    }

    /**
     * Отклоняет выполнение приостановленного конвейера.
     * <p>
     * После вызова, конвейер будет переведен в статус FAILED, и его
     * выполнение будет прекращено.
     *
     * @param executionId ID конвейера, ожидающего утверждения.
     * @return {@link ResponseEntity} со статусом 202 (Accepted).
     */
    @PostMapping("/{executionId}/reject")
    @Operation(summary = "Отклонить выполнение приостановленного шага")
    @ApiResponse(responseCode = "202", description = "Запрос на отклонение принят")
    @ApiResponse(responseCode = "404", description = "Выполнение с таким ID не найдено")
    @ApiResponse(responseCode = "409", description = "Выполнение не ожидает утверждения")
    public ResponseEntity<Void> rejectExecution(
            @Parameter(description = "ID выполнения, полученный от агента") @PathVariable UUID executionId) {
        reviewService.reject(executionId);
        return ResponseEntity.accepted().build();
    }
}
