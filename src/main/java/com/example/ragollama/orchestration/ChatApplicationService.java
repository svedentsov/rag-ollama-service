package com.example.ragollama.orchestration;

import com.example.ragollama.chat.api.dto.ChatRequest;
import com.example.ragollama.chat.api.dto.ChatResponse;
import com.example.ragollama.chat.domain.ChatService;
import com.example.ragollama.chat.domain.model.MessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Сервис прикладного уровня (Application Service), оркестрирующий бизнес-логику чата.
 *
 * <p>Эта версия была отрефакторена для следования принципам Clean Architecture и SRP.
 * Вся логика управления сессиями и историей была делегирована в специализированный
 * сервис {@link DialogManager}. Ответственность этого класса теперь сфокусирована
 * исключительно на оркестрации простого чат-взаимодействия:
 * <ol>
 *     <li>Начать или продолжить диалог через {@link DialogManager}.</li>
 *     <li>Вызвать "чистый" доменный сервис {@link ChatService} для генерации ответа.</li>
 *     <li>Завершить диалог, сохранив ответ ассистента.</li>
 * </ol>
 * Этот подход значительно повышает тестируемость и читаемость кода.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatApplicationService {

    private final ChatService chatService;
    private final DialogManager dialogManager;

    /**
     * Асинхронно обрабатывает чат-запрос, возвращая полный ответ.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link ChatResponse}.
     */
    public CompletableFuture<ChatResponse> processChatRequestAsync(ChatRequest request) {
        // Шаг 1: Начинаем диалог, получаем контекст с историей
        return dialogManager.startTurn(request.sessionId(), request.message(), MessageRole.USER)
                .thenCompose(turnContext ->
                        // Шаг 2: Выполняем основную бизнес-логику (вызов LLM)
                        chatService.processChatRequestAsync(request.message(), turnContext.history())
                                // Шаг 3: Завершаем диалог, сохраняя ответ
                                .thenCompose(llmAnswer ->
                                        dialogManager.endTurn(turnContext.sessionId(), llmAnswer, MessageRole.ASSISTANT)
                                                .thenApply(v -> new ChatResponse(llmAnswer, turnContext.sessionId()))
                                )
                );
    }

    /**
     * Обрабатывает чат-запрос в потоковом режиме, управляя сессией и историей.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link Flux} с текстовыми частями ответа от LLM.
     */
    public Flux<String> processChatRequestStream(ChatRequest request) {
        // Используем Mono.fromFuture для интеграции с асинхронным DialogManager
        return Mono.fromFuture(() -> dialogManager.startTurn(request.sessionId(), request.message(), MessageRole.USER))
                .flatMapMany(turnContext -> {
                    final StringBuilder fullResponseBuilder = new StringBuilder();
                    return chatService.processChatRequestStream(request.message(), turnContext.history())
                            .doOnNext(fullResponseBuilder::append)
                            .doOnComplete(() -> {
                                String fullResponse = fullResponseBuilder.toString();
                                if (!fullResponse.isBlank()) {
                                    dialogManager.endTurn(turnContext.sessionId(), fullResponse, MessageRole.ASSISTANT);
                                }
                            });
                });
    }
}
