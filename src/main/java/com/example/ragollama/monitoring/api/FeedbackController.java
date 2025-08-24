package com.example.ragollama.monitoring.api;

import com.example.ragollama.monitoring.api.dto.FeedbackRequest;
import com.example.ragollama.monitoring.domain.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для приема обратной связи от пользователей.
 */
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback API", description = "API для сбора обратной связи от пользователей")
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Принимает и асинхронно обрабатывает обратную связь по конкретному запросу.
     * <p>
     * Эндпоинт немедленно возвращает ответ `202 Accepted`, подтверждая, что
     * фидбэк принят. Вся дальнейшая обработка (генерация обучающих данных)
     * происходит в фоновом режиме.
     *
     * @param request DTO с идентификатором запроса и оценкой пользователя.
     * @return {@link ResponseEntity} со статусом 202.
     */
    @PostMapping
    @Operation(summary = "Отправить обратную связь по ответу",
            description = "Принимает оценку (полезно/неполезно) для ответа, идентифицированного по X-Request-ID.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Обратная связь принята в обработку."),
                    @ApiResponse(responseCode = "404", description = "Запрос с указанным ID не найден в журнале аудита.")
            })
    public ResponseEntity<Void> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        feedbackService.processFeedback(request);
        return ResponseEntity.accepted().build();
    }
}
