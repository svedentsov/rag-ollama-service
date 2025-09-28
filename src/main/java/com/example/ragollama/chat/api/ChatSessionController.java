package com.example.ragollama.chat.api;

import com.example.ragollama.chat.api.dto.ChatMessageDto;
import com.example.ragollama.chat.api.dto.ChatSessionDto;
import com.example.ragollama.chat.domain.ChatSessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Tag(name = "Chat Session Controller", description = "API для управления сессиями чата")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @GetMapping
    public List<ChatSessionDto> getUserChats() {
        return chatSessionService.getChatsForCurrentUser().stream()
                .map(ChatSessionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatSessionDto createNewChat() {
        return ChatSessionDto.fromEntity(chatSessionService.createNewChat());
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<Void> updateChatName(@PathVariable UUID sessionId, @Valid @RequestBody ChatSessionDto.UpdateRequest request) {
        chatSessionService.updateChatName(sessionId, request.newName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteChat(@PathVariable UUID sessionId) {
        chatSessionService.deleteChat(sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sessionId}/messages")
    public List<ChatMessageDto> getChatMessages(@PathVariable UUID sessionId) {
        return chatSessionService.getMessagesForSession(sessionId).stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }
}
