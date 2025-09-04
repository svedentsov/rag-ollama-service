package com.example.ragollama.orchestration;

import com.example.ragollama.chat.api.dto.ChatRequest;
import com.example.ragollama.chat.api.dto.ChatResponse;
import com.example.ragollama.chat.domain.ChatHistoryService;
import com.example.ragollama.chat.domain.ChatService;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.shared.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис прикладного уровня (Application Service), оркестрирующий бизнес-логику чата.
 * <p>
 * Этот класс является реализацией принципов Clean Architecture и SRP. Его единственная
 * ответственность — управление полным жизненным циклом одного чат-запроса:
 * <ul>
 *     <li>Управление сессией (создание/получение ID).</li>
 *     <li>Сохранение сообщения пользователя в историю.</li>
 *     <li>Извлечение релевантной истории для контекста.</li>
 *     <li>Вызов "чистого" доменного сервиса {@link ChatService} для генерации ответа.</li>
 *     <li>Сохранение ответа AI в историю.</li>
 *     <li>Возврат DTO для Web-слоя.</li>
 * </ul>
 * Сервис является stateless и не зависит от деталей транспортного уровня (HTTP).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatApplicationService {

    private final ChatService chatService;
    private final ChatHistoryService chatHistoryService;
    private final AppProperties appProperties;

    /**
     * Асинхронно обрабатывает чат-запрос, возвращая полный ответ.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link ChatResponse}.
     */
    public CompletableFuture<ChatResponse> processChatRequestAsync(ChatRequest request) {
        final UUID sessionId = getOrCreateSessionId(request.sessionId());

        return saveMessageAndGetHistory(sessionId, request.message())
                .thenCompose(history -> chatService.processChatRequestAsync(request.message(), history))
                .thenCompose(llmAnswer -> saveMessageAsync(sessionId, MessageRole.ASSISTANT, llmAnswer)
                        .thenApply(v -> new ChatResponse(llmAnswer, sessionId)));
    }

    /**
     * Обрабатывает чат-запрос в потоковом режиме, управляя сессией и историей.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link Flux} с текстовыми частями ответа от LLM.
     */
    public Flux<String> processChatRequestStream(ChatRequest request) {
        final UUID sessionId = getOrCreateSessionId(request.sessionId());

        return Flux.defer(() -> Mono.fromFuture(() -> saveMessageAndGetHistory(sessionId, request.message()))
                .flatMapMany(history -> {
                    final StringBuilder fullResponseBuilder = new StringBuilder();
                    return chatService.processChatRequestStream(request.message(), history)
                            .doOnNext(fullResponseBuilder::append)
                            .doOnComplete(() -> {
                                String fullResponse = fullResponseBuilder.toString();
                                if (!fullResponse.isBlank()) {
                                    saveMessageAsync(sessionId, MessageRole.ASSISTANT, fullResponse)
                                            .thenRun(() -> log.debug("Полный потоковый Chat-ответ для сессии {} сохранен.", sessionId));
                                }
                            });
                })
        );
    }

    /**
     * Атомарно сохраняет сообщение пользователя и загружает актуальную историю чата.
     *
     * @param sessionId   ID сессии.
     * @param userMessage Текст сообщения пользователя.
     * @return {@link CompletableFuture} со списком сообщений, включающим текущее.
     */
    private CompletableFuture<List<Message>> saveMessageAndGetHistory(UUID sessionId, String userMessage) {
        final UserMessage currentMessage = new UserMessage(userMessage);

        CompletableFuture<Void> saveFuture = saveMessageAsync(sessionId, MessageRole.USER, userMessage);
        CompletableFuture<List<Message>> historyFuture = getHistoryAsync(sessionId);

        return saveFuture.thenCombine(historyFuture, (v, history) -> {
            List<Message> mutableHistory = new ArrayList<>(history);
            mutableHistory.add(currentMessage);
            return mutableHistory;
        });
    }

    /**
     * Асинхронно сохраняет сообщение в базу данных.
     *
     * @param sessionId ID сессии.
     * @param role      Роль отправителя.
     * @param content   Текст сообщения.
     * @return {@link CompletableFuture}, завершающийся после сохранения.
     */
    private CompletableFuture<Void> saveMessageAsync(UUID sessionId, MessageRole role, String content) {
        return chatHistoryService.saveMessageAsync(sessionId, role, content)
                .exceptionally(ex -> {
                    log.error("Не удалось сохранить сообщение для сессии {}. Роль: {}", sessionId, role, ex);
                    return null; // Игнорируем ошибку, чтобы не прерывать ответ пользователю.
                });
    }

    /**
     * Асинхронно загружает историю сообщений из базы данных.
     *
     * @param sessionId ID сессии.
     * @return {@link CompletableFuture} со списком сообщений.
     */
    private CompletableFuture<List<Message>> getHistoryAsync(UUID sessionId) {
        int maxHistory = appProperties.chat().history().maxMessages();
        // Загружаем N-1 сообщений, так как текущее сообщение пользователя будет добавлено вручную.
        return chatHistoryService.getLastNMessagesAsync(sessionId, maxHistory - 1);
    }

    /**
     * Возвращает ID сессии из запроса или генерирует новый, если он не предоставлен.
     *
     * @param sessionId Опциональный ID из DTO.
     * @return Не-null UUID.
     */
    private UUID getOrCreateSessionId(UUID sessionId) {
        return (sessionId != null) ? sessionId : UUID.randomUUID();
    }
}
