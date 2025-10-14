package com.example.ragollama.orchestration;

import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.example.ragollama.orchestration.handlers.IntentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Обработчик для намерения "Чат", адаптированный для реактивного стека.
 */
@Service
@RequiredArgsConstructor
public class ChatIntentHandler implements IntentHandler {

    private final ChatApplicationService chatApplicationService;

    @Override
    public QueryIntent canHandle() {
        return QueryIntent.CHITCHAT;
    }

    @Override
    public QueryIntent fallbackIntent() {
        return QueryIntent.UNKNOWN;
    }

    @Override
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request, UUID taskId) {
        return chatApplicationService.processChatRequestAsync(request.toChatRequest(), taskId)
                .map(response -> UniversalSyncResponse.from(response, canHandle()))
                .toFuture();
    }

    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request, UUID taskId) {
        return chatApplicationService.processChatRequestStream(request.toChatRequest(), taskId);
    }
}
