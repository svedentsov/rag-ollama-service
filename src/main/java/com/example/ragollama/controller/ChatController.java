package com.example.ragollama.controller;

import com.example.ragollama.dto.ChatRequest;
import com.example.ragollama.dto.ChatResponse;
import com.example.ragollama.service.ChatService;
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

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat Controller", description = "API для прямого взаимодействия с LLM")
public class ChatController {

    private final ChatService chatService;

    /**
     * Отправляет сообщение в чат и получает полный ответ после его генерации.
     *
     * @param chatRequest DTO с сообщением и ID сессии.
     * @return CompletableFuture с полным ответом от LLM.
     */
    @PostMapping
    @Operation(
            summary = "Отправить сообщение в чат (асинхронно)",
            description = "Отправляет сообщение пользователя в AI модель и возвращает полный ответ. Поддерживает сессии для сохранения контекста диалога.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ от AI"),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос (например, пустое сообщение)"),
                    @ApiResponse(responseCode = "422", description = "Обнаружена попытка Prompt Injection"),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")})
    public CompletableFuture<ResponseEntity<ChatResponse>> chat(@Valid @RequestBody ChatRequest chatRequest) {
        return chatService.processChatRequestAsync(chatRequest)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Ошибка при асинхронной обработке чат-запроса", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    /**
     * Отправляет сообщение в чат и получает ответ в виде потока (Server-Sent Events).
     *
     * @param chatRequest DTO с сообщением и ID сессии.
     * @return Реактивный поток {@link Flux}, передающий части ответа по мере их генерации.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Отправить сообщение в чат (потоковый режим)",
            description = "Отправляет сообщение и получает ответ в виде потока Server-Sent Events (SSE). Улучшает UX, показывая ответ по мере генерации.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Поток успешно открыт"),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
                    @ApiResponse(responseCode = "422", description = "Обнаружена попытка Prompt Injection")
            })
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest chatRequest) {
        return chatService.processChatRequestStream(chatRequest);
    }
}
