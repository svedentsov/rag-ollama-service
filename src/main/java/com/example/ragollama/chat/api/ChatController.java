package com.example.ragollama.chat.api;

import com.example.ragollama.chat.api.dto.ChatRequest;
import com.example.ragollama.chat.api.dto.ChatResponse;
import com.example.ragollama.orchestration.ChatApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для стандартного чат-взаимодействия (без RAG).
 * <p>
 * Эталонная реализация контроллера в соответствии с Clean Architecture.
 * Его единственная ответственность — принимать и валидировать DTO,
 * вызывать единственный метод в Application Service и возвращать результат.
 * Он не содержит бизнес-логики и не знает о сессиях, базах данных или
 * деталях работы LLM.
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat Controller", description = "API для простого чата (без RAG)")
public class ChatController {

    private final ChatApplicationService chatApplicationService;

    /**
     * Принимает сообщение от пользователя и возвращает полный, сгенерированный ассистентом ответ.
     * <p> Этот эндпоинт является асинхронным и использует {@link CompletableFuture}
     * для неблокирующей обработки, немедленно освобождая поток веб-сервера.
     *
     * @param request DTO с сообщением пользователя и опциональным ID сессии. Должен быть валидным.
     * @return {@link CompletableFuture} с {@link ChatResponse}, содержащим ответ и ID сессии.
     */
    @PostMapping
    @Operation(summary = "Отправить сообщение в чат (полный ответ)")
    public CompletableFuture<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return chatApplicationService.processChatRequestAsync(request);
    }

    /**
     * Принимает сообщение от пользователя и возвращает ответ в виде потока
     * Server-Sent Events (SSE).
     * <p> Этот эндпоинт обеспечивает низкую задержку первого ответа (time-to-first-token),
     * отправляя фрагменты ответа по мере их генерации языковой моделью.
     *
     * @param request DTO с сообщением пользователя и опциональным ID сессии. Должен быть валидным.
     * @return Реактивный поток {@link Flux}, передающий текстовые фрагменты ответа по мере их генерации.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Отправить сообщение в чат (потоковый ответ)")
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        return chatApplicationService.processChatRequestStream(request);
    }
}
