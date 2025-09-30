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
 * <p>
 * Эта версия была отрефакторена для использования централизованного {@link DialogManager},
 * что устраняет дублирование кода и соответствует принципам Clean Architecture.
 * Она также реализует настоящую сквозную потоковую передачу данных.
 * <p>
 * <b>Ответственности:</b>
 * <ul>
 *     <li>Принимать DTO из слоя контроллеров.</li>
 *     <li>Использовать {@link DialogManager} для управления состоянием диалога.</li>
 *     <li>Делегировать основную бизнес-логику (вызов LLM) доменному сервису {@link ChatService}.</li>
 *     <li>Формировать DTO ответа для контроллера.</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatApplicationService {

    private final ChatService chatService;
    private final DialogManager dialogManager;

    /**
     * Асинхронно обрабатывает чат-запрос, возвращая полный ответ.
     * <p> Конвейер выполнения:
     * <ol>
     *     <li>Вызов {@link DialogManager#startTurn} для получения контекста диалога (ID сессии, история).</li>
     *     <li>Вызов доменного сервиса {@link ChatService} для генерации ответа LLM.</li>
     *     <li>Вызов {@link DialogManager#endTurn} для асинхронного сохранения ответа ассистента.</li>
     *     <li>Формирование и возврат финального DTO {@link ChatResponse}.</li>
     * </ol>
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link ChatResponse}.
     */
    public CompletableFuture<ChatResponse> processChatRequestAsync(ChatRequest request) {
        // Шаг 1: Начинаем диалог, получаем контекст с историей
        return dialogManager.startTurn(request.sessionId(), request.message(), MessageRole.USER)
                .thenCompose(turnContext ->
                        // Шаг 2: Выполняем основную бизнес-логику (вызов LLM)
                        chatService.processChatRequestAsync(turnContext.history())
                                // Шаг 3: Завершаем диалог, сохраняя ответ
                                .thenCompose(llmAnswer ->
                                        dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), llmAnswer, MessageRole.ASSISTANT)
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
        return Mono.fromFuture(() -> dialogManager.startTurn(request.sessionId(), request.message(), MessageRole.USER))
                .flatMapMany(turnContext -> {
                    final StringBuilder fullResponseBuilder = new StringBuilder();
                    return chatService.processChatRequestStream(turnContext.history())
                            .doOnNext(fullResponseBuilder::append)
                            .doOnComplete(() -> {
                                String fullResponse = fullResponseBuilder.toString();
                                if (!fullResponse.isBlank()) {
                                    dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), fullResponse, MessageRole.ASSISTANT);
                                }
                            });
                });
    }
}
