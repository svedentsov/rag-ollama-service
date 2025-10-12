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
import reactor.core.publisher.SignalType;

import java.util.UUID;

/**
 * Сервис-фасад для чата, адаптированный для работы с реактивным DialogManager.
 * <p>
 * Эта версия использует оператор {@code doFinally} для гарантированного сохранения
 * результата в базу данных, даже если поток был прерван клиентом.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatApplicationService {

    private final ChatService chatService;
    private final DialogManager dialogManager;

    /**
     * Асинхронно обрабатывает запрос в чат и возвращает полный ответ.
     *
     * @param request DTO с запросом от пользователя.
     * @param taskId  ID асинхронной задачи, связанной с этим запросом.
     * @return {@link Mono} с полным ответом от чата.
     */
    public Mono<ChatResponse> processChatRequestAsync(ChatRequest request, UUID taskId) {
        return dialogManager.startTurn(request.sessionId(), request.message(), MessageRole.USER)
                .flatMap(turnContext ->
                        chatService.processChatRequestAsync(turnContext.history())
                                .flatMap(llmAnswer ->
                                        dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), llmAnswer, MessageRole.ASSISTANT, taskId)
                                                .thenReturn(new ChatResponse(llmAnswer, turnContext.sessionId()))
                                )
                );
    }

    /**
     * Обрабатывает запрос в чат в потоковом режиме с гарантированным сохранением.
     *
     * @param request DTO с запросом от пользователя.
     * @param taskId  ID асинхронной задачи.
     * @return {@link Flux} с текстовыми фрагментами ответа.
     */
    public Flux<String> processChatRequestStream(ChatRequest request, UUID taskId) {
        return dialogManager.startTurn(request.sessionId(), request.message(), MessageRole.USER)
                .flatMapMany(turnContext -> {
                    final StringBuilder fullResponseBuilder = new StringBuilder();
                    return chatService.processChatRequestStream(turnContext.history())
                            .doOnNext(fullResponseBuilder::append)
                            .doFinally(signalType -> {
                                // Гарантированное сохранение при завершении или отмене
                                if (signalType == SignalType.ON_COMPLETE || signalType == SignalType.CANCEL) {
                                    String fullResponse = fullResponseBuilder.toString();
                                    if (!fullResponse.isBlank()) {
                                        dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), fullResponse, MessageRole.ASSISTANT, taskId)
                                                .subscribe(null, error -> log.error("Ошибка при сохранении прерванного Chat-ответа", error));
                                    }
                                }
                            });
                });
    }
}
