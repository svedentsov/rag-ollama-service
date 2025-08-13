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
 * Контроллер для простого диалога с AI без RAG.
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat Controller", description = "API для прямого взаимодействия с LLM")
public class ChatController {

    private final ChatService chatService;

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
