package com.example.ragollama.controller;

import com.example.ragollama.dto.ChatRequest;
import com.example.ragollama.dto.ChatResponse;
import com.example.ragollama.service.ChatService;
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
 * Контроллер для прямого диалога с LLM без использования RAG.
 * <p>
 * Предоставляет эндпоинт для простого чата с AI, поддерживая
 * контекст диалога через идентификатор сессии.
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat Controller", description = "API для прямого взаимодействия с LLM")
public class ChatController {

    private final ChatService chatService;

    /**
     * Обрабатывает сообщение от пользователя, отправляет его в AI модель и возвращает ответ.
     * <p>
     * Если в запросе указан {@code sessionId}, диалог продолжается в рамках существующей сессии.
     * Если {@code sessionId} не указан, создается новая сессия. История диалога сохраняется в базе данных.
     *
     * @param chatRequest DTO с сообщением пользователя и опциональным ID сессии.
     * @return {@link ResponseEntity} с DTO {@link ChatResponse}, содержащим ответ AI и ID сессии.
     */
    @PostMapping
    @Operation(
            summary = "Отправить сообщение в чат",
            description = "Отправляет сообщение пользователя в AI модель и возвращает ответ. Поддерживает сессии для сохранения контекста диалога.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ от AI"),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос (например, пустое сообщение)"),
                    @ApiResponse(responseCode = "422", description = "Обнаружена попытка Prompt Injection")})
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest chatRequest) {
        ChatResponse response = chatService.processChatRequest(chatRequest);
        return ResponseEntity.ok(response);
    }
}
