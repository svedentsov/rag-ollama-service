package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.api.dto.ChatRequest;
import com.example.ragollama.chat.api.dto.ChatResponse;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.security.PromptGuardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для обработки бизнес-логики чата, полностью перестроенный на
 * явную асинхронную модель с использованием {@link CompletableFuture} и {@link AsyncTaskExecutor}.
 * <p>
 * Этот сервис является оркестратором, который явно делегирует блокирующие
 * I/O-операции (взаимодействие с {@link ChatHistoryService}) на выделенный
 * пул потоков, предотвращая блокировку основных потоков приложения.
 * Это обеспечивает максимальную производительность и отзывчивость API.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final LlmClient llmClient;
    private final ChatHistoryService chatHistoryService;
    private final PromptGuardService promptGuardService;
    private final AppProperties appProperties;
    private final AsyncTaskExecutor applicationTaskExecutor;

    /**
     * Внутренний record для передачи подготовленного контекста между методами.
     */
    private record ChatContext(Prompt prompt, UUID sessionId) {
    }

    /**
     * Обрабатывает чат-запрос асинхронно, возвращая полный ответ.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link ChatResponse}.
     */
    public CompletableFuture<ChatResponse> processChatRequestAsync(ChatRequest request) {
        return prepareChatContextAsync(request)
                .thenCompose(context -> {
                    log.info("Обработка асинхронного запроса в чат для сессии ID: {}", context.sessionId());
                    return llmClient.callChat(context.prompt())
                            .thenCompose(aiResponseContent -> {
                                log.debug("Получен полный ответ AI для сессии {}.", context.sessionId());
                                // Асинхронно сохраняем ответ ассистента в БД
                                return CompletableFuture.runAsync(
                                        () -> chatHistoryService.saveMessage(context.sessionId(), MessageRole.ASSISTANT, aiResponseContent),
                                        applicationTaskExecutor
                                ).thenApply(v -> new ChatResponse(aiResponseContent, context.sessionId()));
                            });
                });
    }

    /**
     * Обрабатывает чат-запрос в потоковом режиме (Server-Sent Events).
     *
     * @param request DTO с запросом от пользователя.
     * @return Реактивный поток {@link Flux}, передающий части ответа по мере их генерации.
     */
    public Flux<String> processChatRequestStream(ChatRequest request) {
        return Flux.defer(() -> Mono.fromFuture(() -> prepareChatContextAsync(request))
                .flatMapMany(context -> {
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
                                    // Асинхронный "fire-and-forget" вызов для сохранения истории
                                    CompletableFuture.runAsync(
                                            () -> chatHistoryService.saveMessage(context.sessionId(), MessageRole.ASSISTANT, fullResponse),
                                            applicationTaskExecutor
                                    ).thenRun(() -> log.debug("Полный потоковый ответ для сессии {} сохранен.", context.sessionId()));
                                } else {
                                    log.warn("Потоковый ответ для сессии {} был пустым. История не сохранена.", context.sessionId());
                                }
                            })
                            .doOnError(error -> log.error("Ошибка в потоке чата для сессии {}:", context.sessionId(), error));
                })
        );
    }

    /**
     * Асинхронно подготавливает контекст для чата: валидирует ввод, управляет сессией,
     * сохраняет сообщение пользователя и формирует финальный промпт с историей.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture}, содержащий промпт и ID сессии.
     */
    private CompletableFuture<ChatContext> prepareChatContextAsync(ChatRequest request) {
        try {
            promptGuardService.checkForInjection(request.message());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
        final UUID sessionId = (request.sessionId() != null) ? request.sessionId() : UUID.randomUUID();
        final int maxHistory = appProperties.chat().history().maxMessages();

        // Шаг 1: Асинхронно сохраняем сообщение пользователя в БД.
        return CompletableFuture.runAsync(
                        () -> chatHistoryService.saveMessage(sessionId, MessageRole.USER, request.message()),
                        applicationTaskExecutor)
                // Шаг 2: После сохранения, асинхронно загружаем историю сообщений.
                .thenCompose(v -> CompletableFuture.supplyAsync(
                        () -> chatHistoryService.getLastNMessages(sessionId, maxHistory),
                        applicationTaskExecutor))
                // Шаг 3: Когда история загружена, создаем промпт и контекст.
                .thenApply(chatHistory -> {
                    Prompt prompt = new Prompt(chatHistory);
                    return new ChatContext(prompt, sessionId);
                });
    }
}
