package com.example.ragollama.chat.api;

import com.example.ragollama.chat.api.dto.ChatMessageDto;
import com.example.ragollama.chat.api.dto.ChatSessionDto;
import com.example.ragollama.chat.domain.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Контроллер для управления жизненным циклом сессий чата.
 * <p>
 * Предоставляет REST API для создания, получения списка, переименования
 * и удаления чат-сессий, а также для получения сообщений в рамках
 * конкретной сессии. Вся бизнес-логика делегируется
 * специализированному сервису {@link ChatSessionService}.
 */
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Tag(name = "Chat Session Controller", description = "API для управления сессиями чата")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * Возвращает список всех чат-сессий для текущего пользователя.
     *
     * @return Список DTO {@link ChatSessionDto}.
     */
    @GetMapping
    @Operation(summary = "Получить список всех чатов пользователя")
    public List<ChatSessionDto> getUserChats() {
        return chatSessionService.getChatsForCurrentUser().stream()
                .map(ChatSessionDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Создает новую, пустую сессию чата.
     *
     * @return DTO {@link ChatSessionDto} для созданной сессии.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать новую сессию чата")
    public ChatSessionDto createNewChat() {
        return ChatSessionDto.fromEntity(chatSessionService.createNewChat());
    }

    /**
     * Обновляет имя существующей сессии чата.
     *
     * @param sessionId ID сессии для переименования.
     * @param request   DTO с новым именем.
     * @return {@link ResponseEntity} со статусом 200 OK.
     */
    @PutMapping("/{sessionId}")
    @Operation(summary = "Переименовать сессию чата")
    public ResponseEntity<Void> updateChatName(@PathVariable UUID sessionId, @Valid @RequestBody ChatSessionDto.UpdateRequest request) {
        chatSessionService.updateChatName(sessionId, request.newName());
        return ResponseEntity.ok().build();
    }

    /**
     * Удаляет сессию чата и все связанные с ней сообщения.
     *
     * @param sessionId ID сессии для удаления.
     * @return {@link ResponseEntity} со статусом 204 No Content.
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Удалить сессию чата")
    public ResponseEntity<Void> deleteChat(@PathVariable UUID sessionId) {
        chatSessionService.deleteChat(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Возвращает историю сообщений для указанной сессии.
     *
     * @param sessionId ID сессии.
     * @return Список DTO {@link ChatMessageDto}.
     */
    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "Получить историю сообщений для сессии")
    public List<ChatMessageDto> getChatMessages(@PathVariable UUID sessionId) {
        return chatSessionService.getMessagesForSession(sessionId).stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }
}
