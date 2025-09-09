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
 * Обработчик, реализующий логику для намерения {@link QueryIntent#RAG_QUERY}.
 * <p>
 * Этот компонент является частью паттерна "Стратегия". Он инкапсулирует
 * вызов {@link RagApplicationService} и преобразование его результата в
 * унифицированный формат ответа.
 */
@Service
@RequiredArgsConstructor
public class RagIntentHandler implements IntentHandler {

    private final RagApplicationService ragApplicationService;

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryIntent canHandle() {
        return QueryIntent.RAG_QUERY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request) {
        return ragApplicationService.processRagRequestAsync(request.toRagQueryRequest())
                .thenApply(response -> UniversalSyncResponse.from(response, canHandle()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request) {
        return ragApplicationService.processRagRequestStream(request.toRagQueryRequest())
                .map(UniversalResponse::from);
    }
}
