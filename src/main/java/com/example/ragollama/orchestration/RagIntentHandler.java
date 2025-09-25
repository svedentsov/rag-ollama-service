package com.example.ragollama.orchestration;

import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.example.ragollama.orchestration.handlers.IntentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagIntentHandler implements IntentHandler {

    private final RagApplicationService ragApplicationService;

    @Override
    public QueryIntent canHandle() {
        return QueryIntent.RAG_QUERY;
    }

    @Override
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request) {
        return ragApplicationService.processRagRequestAsync(request.toRagQueryRequest())
                .thenApply(response -> UniversalSyncResponse.from(response, canHandle()));
    }

    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request) {
        return ragApplicationService.processRagRequestStream(request.toRagQueryRequest())
                .map(UniversalResponse::from)
                .onErrorResume(e -> { // !!! ДОБАВЛЕНО !!!
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    if (cause instanceof CancellationException || cause instanceof IOException) {
                        log.warn("Поток RAG был прерван клиентом: {}", cause.getMessage());
                        return Flux.empty();
                    }
                    log.error("Ошибка в потоке RAG: {}", e.getMessage(), e);
                    return Flux.just(new UniversalResponse.Error("Ошибка при обработке RAG-запроса: " + e.getMessage()));
                });
    }
}
