package com.example.ragollama.orchestration;

import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.example.ragollama.orchestration.handlers.IntentHandler;
import com.example.ragollama.summarization.SummarizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * Обработчик, реализующий логику для намерения {@link QueryIntent#SUMMARIZATION}.
 * <p>
 * Этот компонент является частью паттерна "Стратегия". Он инкапсулирует
 * вызов {@link SummarizationService} и преобразование его результата в
 * унифицированный формат ответа.
 */
@Service
@RequiredArgsConstructor
public class SummarizationIntentHandler implements IntentHandler {

    private final SummarizationService summarizationService;

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryIntent canHandle() {
        return QueryIntent.SUMMARIZATION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request) {
        return summarizationService.summarizeAsync(request.context(), null)
                .map(summary -> UniversalSyncResponse.from(summary, canHandle()))
                .toFuture();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request) {
        return summarizationService.summarizeAsync(request.context(), null)
                .map(UniversalResponse::from)
                .flux();
    }
}
