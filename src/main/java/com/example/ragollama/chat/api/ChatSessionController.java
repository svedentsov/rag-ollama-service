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
 */
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Tag(name = "Chat Session Controller", description = "API для управления сессиями чата")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @GetMapping
    @Operation(summary = "Получить список всех чатов пользователя")
    public List<ChatSessionDto> getUserChats() {
        return chatSessionService.getChatsForCurrentUser().stream()
                .map(ChatSessionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать новую сессию чата")
    public ChatSessionDto createNewChat() {
        return ChatSessionDto.fromEntity(chatSessionService.createNewChat());
    }

    @PutMapping("/{sessionId}")
    @Operation(summary = "Переименовать сессию чата")
    public ResponseEntity<Void> updateChatName(@PathVariable UUID sessionId, @Valid @RequestBody ChatSessionDto.UpdateRequest request) {
        chatSessionService.updateChatName(sessionId, request.newName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Удалить сессию чата")
    public ResponseEntity<Void> deleteChat(@PathVariable UUID sessionId) {
        chatSessionService.deleteChat(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Возвращает историю сообщений для указанной сессии.
     * Теперь использует обновленный ChatMessageDto, включающий parentId.
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
