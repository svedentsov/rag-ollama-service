package com.example.ragollama.service;

import com.example.ragollama.dto.ChatRequest;
import com.example.ragollama.dto.ChatResponse;
import com.example.ragollama.entity.MessageRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ResilientOllamaClient resilientOllamaClient;
    private final ChatHistoryService chatHistoryService;
    private final PromptGuardService promptGuardService;
    private final AsyncTaskExecutor taskExecutor;
    private final int maxHistoryMessages;

    public ChatService(ResilientOllamaClient resilientOllamaClient,
                       ChatHistoryService chatHistoryService,
                       PromptGuardService promptGuardService,
                       AsyncTaskExecutor taskExecutor, // Эта зависимость теперь будет найдена
                       @Value("${app.chat.history.max-messages}") int maxHistoryMessages) {
        this.resilientOllamaClient = resilientOllamaClient;
        this.chatHistoryService = chatHistoryService;
        this.promptGuardService = promptGuardService;
        this.taskExecutor = taskExecutor;
        this.maxHistoryMessages = maxHistoryMessages;
    }

    public CompletableFuture<ChatResponse> processChatRequestAsync(ChatRequest request) {
        promptGuardService.checkForInjection(request.message());
        final UUID sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID();
        log.info("Обработка запроса в чат для сессии ID: {}. Глубина истории: {}", sessionId, maxHistoryMessages);

        chatHistoryService.saveMessage(sessionId, MessageRole.USER, request.message());

        List<Message> chatHistory = chatHistoryService.getLastNMessages(sessionId, maxHistoryMessages);

        Prompt prompt = new Prompt(chatHistory);

        return resilientOllamaClient.callChat(prompt)
                .thenApplyAsync(aiResponseContent -> {
                    log.debug("Получен ответ AI для сессии {}: '{}'", sessionId, aiResponseContent);
                    chatHistoryService.saveMessage(sessionId, MessageRole.ASSISTANT, aiResponseContent);
                    return new ChatResponse(aiResponseContent, sessionId);
                }, taskExecutor);
    }
}