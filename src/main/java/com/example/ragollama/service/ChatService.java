package com.example.ragollama.service;

import com.example.ragollama.dto.ChatRequest;
import com.example.ragollama.dto.ChatResponse;
import com.example.ragollama.entity.ChatMessage;
import com.example.ragollama.entity.MessageRole;
import com.example.ragollama.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сервис для управления диалогом с AI.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final ChatMessageRepository chatMessageRepository;
    private final PromptGuardService promptGuardService;

    @Transactional
    public ChatResponse processChatRequest(ChatRequest request) {
        promptGuardService.checkForInjection(request.message());

        UUID sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID();
        log.info("Processing chat request for session ID: {}", sessionId);

        saveMessage(sessionId, MessageRole.USER, request.message());

        Prompt prompt = new Prompt(request.message());
        // fluent API
        String aiResponseContent = chatClient.prompt(prompt)
                .call()
                .content();

        log.debug("AI response for session {}: {}", sessionId, aiResponseContent);

        saveMessage(sessionId, MessageRole.ASSISTANT, aiResponseContent);

        return new ChatResponse(aiResponseContent, sessionId);
    }

    private void saveMessage(UUID sessionId, MessageRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(message);
        log.debug("Saved message for session {}: Role={}, Content='{}'", sessionId, role,
                content == null ? "" : content.substring(0, Math.min(content.length(), 50)) + "...");
    }
}
