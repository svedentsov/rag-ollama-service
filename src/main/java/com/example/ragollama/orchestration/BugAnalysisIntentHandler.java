package com.example.ragollama.orchestration;

import com.example.ragollama.agent.buganalysis.domain.BugAnalysisService;
import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.example.ragollama.orchestration.handlers.IntentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Обработчик, реализующий логику для намерения {@link QueryIntent#BUG_ANALYSIS}.
 * <p>
 * Этот компонент является частью паттерна "Стратегия". Он инкапсулирует
 * вызов {@link BugAnalysisService} и преобразование его результата в
 * унифицированный формат ответа.
 */
@Service
@RequiredArgsConstructor
public class BugAnalysisIntentHandler implements IntentHandler {

    private final BugAnalysisService bugAnalysisService;

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryIntent canHandle() {
        return QueryIntent.BUG_ANALYSIS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request) {
        return bugAnalysisService.analyzeBugReport(request.query())
                .thenApply(response -> UniversalSyncResponse.from(response, canHandle()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request) {
        return Mono.fromFuture(() -> bugAnalysisService.analyzeBugReport(request.query()))
                .map(UniversalResponse::from)
                .flux();
    }
}
