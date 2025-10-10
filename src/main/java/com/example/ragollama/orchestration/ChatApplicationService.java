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

import java.util.UUID;

/**
 * Сервис-фасад для чата, адаптированный для работы с реактивным DialogManager.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatApplicationService {

    private final ChatService chatService;
    private final DialogManager dialogManager;

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

    public Flux<String> processChatRequestStream(ChatRequest request, UUID taskId) {
        return dialogManager.startTurn(request.sessionId(), request.message(), MessageRole.USER)
                .flatMapMany(turnContext -> {
                    final StringBuilder fullResponseBuilder = new StringBuilder();
                    return chatService.processChatRequestStream(turnContext.history())
                            .doOnNext(fullResponseBuilder::append)
                            .doOnComplete(() -> {
                                String fullResponse = fullResponseBuilder.toString();
                                if (!fullResponse.isBlank()) {
                                    dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), fullResponse, MessageRole.ASSISTANT, taskId).subscribe();
                                }
                            });
                });
    }
}
