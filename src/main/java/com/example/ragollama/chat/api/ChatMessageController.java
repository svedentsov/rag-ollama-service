package com.example.ragollama.chat.api;

import com.example.ragollama.chat.api.dto.UpdateMessageRequest;
import com.example.ragollama.chat.domain.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Контроллер для управления отдельными сообщениями в чате.
 * <p>
 * Предоставляет эндпоинты для редактирования и удаления сообщений,
 * делегируя всю бизнес-логику и проверку прав доступа
 * специализированному сервису {@link ChatMessageService}.
 */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Tag(name = "Chat Message Controller", description = "API для управления отдельными сообщениями чата")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    /**
     * Обновляет текст существующего сообщения.
     *
     * @param messageId ID сообщения для обновления.
     * @param request   DTO с новым содержимым сообщения.
     * @return {@link ResponseEntity} со статусом 200 OK в случае успеха.
     */
    @PutMapping("/{messageId}")
    @Operation(summary = "Обновить сообщение пользователя")
    public ResponseEntity<Void> updateMessage(
            @PathVariable UUID messageId,
            @Valid @RequestBody UpdateMessageRequest request) {
        chatMessageService.updateMessage(messageId, request.newContent());
        return ResponseEntity.ok().build();
    }

    /**
     * Удаляет существующее сообщение.
     *
     * @param messageId ID сообщения для удаления.
     * @return {@link ResponseEntity} со статусом 204 No Content в случае успеха.
     */
    @DeleteMapping("/{messageId}")
    @Operation(summary = "Удалить сообщение пользователя")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId) {
        chatMessageService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }
}
