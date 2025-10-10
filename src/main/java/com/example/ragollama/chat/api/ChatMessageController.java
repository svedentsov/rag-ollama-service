package com.example.ragollama.chat.api;

import com.example.ragollama.chat.api.dto.UpdateMessageRequest;
import com.example.ragollama.chat.domain.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Контроллер для управления отдельными сообщениями в чате.
 * <p>
 * Предоставляет эндпоинты для редактирования и удаления сообщений,
 * делегируя всю бизнес-логику и проверку прав доступа
 * специализированному сервису {@link ChatMessageService}.
 * Эта версия корректно обрабатывает асинхронные операции в
 * реактивном стиле.
 */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Tag(name = "Chat Message Controller", description = "API для управления отдельными сообщениями чата")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    /**
     * Асинхронно обновляет текст существующего сообщения.
     * <p>
     * Этот метод возвращает {@link Mono}, что позволяет Spring WebFlux
     * дождаться завершения асинхронной операции в базе данных перед
     * отправкой HTTP-ответа клиенту.
     *
     * @param messageId ID сообщения для обновления.
     * @param request   DTO с новым содержимым сообщения.
     * @return {@link Mono}, который по успешному завершению эммитит
     *         {@link ResponseEntity} со статусом 200 OK.
     */
    @PutMapping("/{messageId}")
    @Operation(summary = "Обновить сообщение пользователя")
    public Mono<ResponseEntity<Void>> updateMessage(
            @PathVariable UUID messageId,
            @Valid @RequestBody UpdateMessageRequest request) {
        return chatMessageService.updateMessage(messageId, request.newContent())
                .thenReturn(ResponseEntity.ok().build());
    }

    /**
     * Асинхронно удаляет существующее сообщение.
     * <p>
     * Возвращает {@link Mono} для корректной интеграции с реактивным
     * конвейером Spring WebFlux.
     *
     * @param messageId ID сообщения для удаления.
     * @return {@link Mono}, который по успешному завершению эммитит
     *         {@link ResponseEntity} со статусом 204 No Content.
     */
    @DeleteMapping("/{messageId}")
    @Operation(summary = "Удалить сообщение пользователя")
    public Mono<ResponseEntity<Void>> deleteMessage(@PathVariable UUID messageId) {
        return chatMessageService.deleteMessage(messageId)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
