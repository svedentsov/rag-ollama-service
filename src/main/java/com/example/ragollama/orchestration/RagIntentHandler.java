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
import reactor.core.publisher.SignalType;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Обработчик для намерения "RAG", адаптированный для реактивного стека.
 * <p>
 * Эта версия использует оператор {@code doFinally} для гарантированного сохранения
 * результата в базу данных, даже если поток был прерван клиентом.
 */
@Service
@Slf4j
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
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request, UUID taskId) {
        return ragApplicationService.processRagRequestAsync(request.toRagQueryRequest(), taskId)
                .map(response -> UniversalSyncResponse.from(response, canHandle()))
                .toFuture();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request, UUID taskId) {
        return ragApplicationService.processRagRequestStream(request.toRagQueryRequest(), taskId)
                .map(UniversalResponse::from)
                .onErrorResume(e -> {
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
