package com.example.ragollama.orchestration;

import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.example.ragollama.orchestration.handlers.IntentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * Обработчик, реализующий логику для намерения {@link QueryIntent#CHITCHAT} и {@link QueryIntent#UNKNOWN}.
 * <p>
 * Этот компонент является частью паттерна "Стратегия". Он инкапсулирует
 * вызов {@link ChatApplicationService} и преобразование его результата в
 * унифицированный формат ответа.
 */
@Service
@RequiredArgsConstructor
public class ChatIntentHandler implements IntentHandler {

    private final ChatApplicationService chatApplicationService;

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryIntent canHandle() {
        return QueryIntent.CHITCHAT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryIntent fallbackIntent() {
        return QueryIntent.UNKNOWN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request) {
        return chatApplicationService.processChatRequestAsync(request.toChatRequest())
                .thenApply(response -> UniversalSyncResponse.from(response, canHandle()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request) {
        return chatApplicationService.processChatRequestStream(request.toChatRequest())
                .map(UniversalResponse::from);
    }
}
