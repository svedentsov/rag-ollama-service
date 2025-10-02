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
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatApplicationService {

    private final ChatService chatService;
    private final DialogManager dialogManager;

    public CompletableFuture<ChatResponse> processChatRequestAsync(ChatRequest request, UUID taskId) {
        return dialogManager.startTurn(request.sessionId(), request.message(), MessageRole.USER)
                .thenCompose(turnContext ->
                        chatService.processChatRequestAsync(turnContext.history())
                                .thenCompose(llmAnswer ->
                                        dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), llmAnswer, MessageRole.ASSISTANT, taskId)
                                                .thenApply(v -> new ChatResponse(llmAnswer, turnContext.sessionId()))
                                )
                );
    }

    public Flux<String> processChatRequestStream(ChatRequest request, UUID taskId) {
        return Mono.fromFuture(() -> dialogManager.startTurn(request.sessionId(), request.message(), MessageRole.USER))
                .flatMapMany(turnContext -> {
                    final StringBuilder fullResponseBuilder = new StringBuilder();
                    return chatService.processChatRequestStream(turnContext.history())
                            .doOnNext(fullResponseBuilder::append)
                            .doOnComplete(() -> {
                                String fullResponse = fullResponseBuilder.toString();
                                if (!fullResponse.isBlank()) {
                                    dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), fullResponse, MessageRole.ASSISTANT, taskId);
                                }
                            });
                });
    }
}
