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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Контроллер для управления жизненным циклом сессий чата, адаптированный для WebFlux.
 * <p>
 * Эталонная реализация, где все методы, выполняющие изменяющие операции,
 * возвращают {@link Mono} для корректной обработки в реактивном стеке.
 */
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Tag(name = "Chat Session Controller", description = "API для управления сессиями чата")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * Получает список всех сессий чата для текущего пользователя.
     *
     * @return {@link Flux} с DTO сессий чата.
     */
    @GetMapping
    @Operation(summary = "Получить список всех чатов пользователя")
    public Flux<ChatSessionDto> getUserChats() {
        return chatSessionService.getChatsForCurrentUser()
                .map(ChatSessionDto::fromEntity);
    }

    /**
     * Создает новую сессию чата.
     *
     * @return {@link Mono} с DTO созданной сессии.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать новую сессию чата")
    public Mono<ChatSessionDto> createNewChat() {
        return chatSessionService.createNewChat()
                .map(ChatSessionDto::fromEntity);
    }

    /**
     * Асинхронно переименовывает сессию чата.
     *
     * @param sessionId ID сессии.
     * @param request   DTO с новым именем.
     * @return {@link Mono} с {@link ResponseEntity}.
     */
    @PutMapping("/{sessionId}")
    @Operation(summary = "Переименовать сессию чата")
    public Mono<ResponseEntity<Void>> updateChatName(@PathVariable UUID sessionId, @Valid @RequestBody ChatSessionDto.UpdateRequest request) {
        return chatSessionService.updateChatName(sessionId, request.newName())
                .thenReturn(ResponseEntity.ok().build());
    }

    /**
     * Асинхронно устанавливает активную ветку для ответа в диалоге.
     *
     * @param sessionId ID сессии.
     * @param request   DTO с ID родительского и дочернего сообщений.
     * @return {@link Mono} с {@link ResponseEntity}.
     */
    @PutMapping("/{sessionId}/active-branch")
    @Operation(summary = "Выбрать активную ветку для ответа")
    public Mono<ResponseEntity<Void>> setActiveBranch(@PathVariable UUID sessionId, @Valid @RequestBody ChatSessionDto.UpdateActiveBranchRequest request) {
        return chatSessionService.setActiveBranch(sessionId, request.parentMessageId(), request.activeChildId())
                .thenReturn(ResponseEntity.ok().build());
    }

    /**
     * Асинхронно удаляет сессию чата и все связанные с ней сообщения.
     *
     * @param sessionId ID сессии для удаления.
     * @return {@link Mono} с {@link ResponseEntity}.
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Удалить сессию чата")
    public Mono<ResponseEntity<Void>> deleteChat(@PathVariable UUID sessionId) {
        return chatSessionService.deleteChat(sessionId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    /**
     * Получает историю сообщений для указанной сессии.
     *
     * @param sessionId ID сессии.
     * @return {@link Flux} с DTO сообщений.
     */
    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "Получить историю сообщений для сессии")
    public Flux<ChatMessageDto> getChatMessages(@PathVariable UUID sessionId) {
        return chatSessionService.getMessagesForSession(sessionId);
    }
}
