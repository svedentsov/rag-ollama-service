package com.example.ragollama.chat.domain;

import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.chat.api.dto.ChatRequest;
import com.example.ragollama.chat.api.dto.ChatResponse;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.shared.security.PromptGuardService;
import com.example.ragollama.shared.llm.LlmClient;
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
 * Эта версия устраняет дублирование кода путем вынесения общей логики
 * подготовки (валидация, управление сессией, загрузка истории)
 * в приватный метод {@link #prepareChatContext(ChatRequest)}.
 * Публичные методы теперь отвечают только за свою основную задачу:
 * либо получить полный ответ, либо организовать потоковую передачу.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final LlmClient llmClient;
    private final ChatHistoryService chatHistoryService;
    private final PromptGuardService promptGuardService;
    private final AppProperties appProperties;

    /**
     * Внутренний record для передачи подготовленного контекста между методами.
     *
     * @param prompt    Готовый к отправке промпт с историей сообщений.
     * @param sessionId Идентификатор текущей сессии чата.
     */
    private record ChatContext(Prompt prompt, UUID sessionId) {}

    /**
     * Обрабатывает чат-запрос асинхронно, возвращая полный ответ.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link ChatResponse}.
     */
    public CompletableFuture<ChatResponse> processChatRequestAsync(ChatRequest request) {
        ChatContext context = prepareChatContext(request);
        log.info("Обработка асинхронного запроса в чат для сессии ID: {}", context.sessionId());

        return llmClient.callChat(context.prompt())
                .thenApply(aiResponseContent -> {
                    log.debug("Получен полный ответ AI для сессии {}.", context.sessionId());
                    chatHistoryService.saveMessage(context.sessionId(), MessageRole.ASSISTANT, aiResponseContent);
                    return new ChatResponse(aiResponseContent, context.sessionId());
                });
    }

    /**
     * Обрабатывает чат-запрос в потоковом режиме (Server-Sent Events).
     *
     * @param request DTO с запросом от пользователя.
     * @return Реактивный поток {@link Flux}, передающий части ответа по мере их генерации.
     */
    public Flux<String> processChatRequestStream(ChatRequest request) {
        ChatContext context = prepareChatContext(request);
        log.info("Обработка потокового запроса в чат для сессии ID: {}", context.sessionId());

        final StringBuilder fullResponseBuilder = new StringBuilder();
        return llmClient.streamChat(context.prompt())
                .doOnNext(chunk -> {
                    log.trace("Сессия {}: получен чанк.", context.sessionId());
                    fullResponseBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    String fullResponse = fullResponseBuilder.toString();
                    if (!fullResponse.isBlank()) {
                        chatHistoryService.saveMessage(context.sessionId(), MessageRole.ASSISTANT, fullResponse);
                        log.debug("Полный потоковый ответ для сессии {} сохранен.", context.sessionId());
                    } else {
                        log.warn("Потоковый ответ для сессии {} был пустым. История не сохранена.", context.sessionId());
                    }
                })
                .doOnError(error -> log.error("Ошибка в потоке чата для сессии {}:", context.sessionId(), error));
    }

    /**
     * Подготавливает контекст для чата: валидирует ввод, управляет сессией,
     * сохраняет сообщение пользователя и формирует финальный промпт с историей.
     *
     * @param request DTO с запросом от пользователя.
     * @return Объект {@link ChatContext}, содержащий промпт и ID сессии.
     */
    private ChatContext prepareChatContext(ChatRequest request) {
        promptGuardService.checkForInjection(request.message());

        final UUID sessionId = (request.sessionId() != null) ? request.sessionId() : UUID.randomUUID();
        final int maxHistory = appProperties.chat().history().maxMessages();

        chatHistoryService.saveMessage(sessionId, MessageRole.USER, request.message());

        List<Message> chatHistory = chatHistoryService.getLastNMessages(sessionId, maxHistory);
        Prompt prompt = new Prompt(chatHistory);

        return new ChatContext(prompt, sessionId);
    }
}
