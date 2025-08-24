package com.example.ragollama.qaagent.review;

import io.swagger.v3.oas.annotations.Operation;
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
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Review API", description = "API для утверждения или отклонения шагов конвейера")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Утверждает выполнение приостановленного конвейера.
     *
     * @param executionId ID конвейера, ожидающего утверждения.
     * @return {@link ResponseEntity} со статусом 202 (Accepted).
     */
    @PostMapping("/{executionId}/approve")
    @Operation(summary = "Утвердить приостановленный шаг")
    public ResponseEntity<Void> approveExecution(@PathVariable UUID executionId) {
        reviewService.approve(executionId);
        return ResponseEntity.accepted().build();
    }
}
