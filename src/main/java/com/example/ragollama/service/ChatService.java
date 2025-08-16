package com.example.ragollama.service;

import com.example.ragollama.config.properties.AppProperties;
import com.example.ragollama.dto.ChatRequest;
import com.example.ragollama.dto.ChatResponse;
import com.example.ragollama.entity.MessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для обработки бизнес-логики чата.
 * Отвечает за взаимодействие с LLM, управление историей диалога
 * и проверку пользовательского ввода на безопасность.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ResilientOllamaClient resilientOllamaClient;
    private final ChatHistoryService chatHistoryService;
    private final PromptGuardService promptGuardService;
    private final AppProperties appProperties;

    /**
     * Обрабатывает чат-запрос асинхронно, возвращая полный ответ.
     *
     * @param request DTO с запросом.
     * @return CompletableFuture с финальным ответом.
     */
    public CompletableFuture<ChatResponse> processChatRequestAsync(ChatRequest request) {
        promptGuardService.checkForInjection(request.message());
        final UUID sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID();
        final int maxHistory = appProperties.chat().history().maxMessages();
        log.info("Обработка запроса в чат для сессии ID: {}. Глубина истории: {}", sessionId, maxHistory);
        chatHistoryService.saveMessage(sessionId, MessageRole.USER, request.message());
        List<Message> chatHistory = chatHistoryService.getLastNMessages(sessionId, maxHistory);
        Prompt prompt = new Prompt(chatHistory);
        return resilientOllamaClient.callChat(prompt)
                .thenApply(aiResponseContent -> {
                    log.debug("Получен ответ AI для сессии {}: '{}'", sessionId, aiResponseContent);
                    chatHistoryService.saveMessage(sessionId, MessageRole.ASSISTANT, aiResponseContent);
                    return new ChatResponse(aiResponseContent, sessionId);
                });
    }

    /**
     * Обрабатывает чат-запрос в потоковом режиме.
     *
     * @param request DTO с запросом.
     * @return Реактивный поток {@link Flux} с частями ответа.
     */
    public Flux<String> processChatRequestStream(ChatRequest request) {
        promptGuardService.checkForInjection(request.message());
        final UUID sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID();
        final int maxHistory = appProperties.chat().history().maxMessages();
        log.info("Обработка потокового запроса в чат для сессии ID: {}", sessionId);
        chatHistoryService.saveMessage(sessionId, MessageRole.USER, request.message());
        List<Message> chatHistory = chatHistoryService.getLastNMessages(sessionId, maxHistory);
        Prompt prompt = new Prompt(chatHistory);
        return resilientOllamaClient.streamChat(prompt)
                .doOnNext(chunk -> log.trace("Сессия {}: получен чанк '{}'", sessionId, chunk))
                .collectList()
                .doOnSuccess(chunks -> {
                    String fullResponse = String.join("", chunks);
                    chatHistoryService.saveMessage(sessionId, MessageRole.ASSISTANT, fullResponse);
                    log.debug("Полный потоковый ответ для сессии {} сохранен.", sessionId);
                })
                .flatMapMany(Flux::fromIterable);
    }
}
