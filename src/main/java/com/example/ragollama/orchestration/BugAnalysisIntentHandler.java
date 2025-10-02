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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class BugAnalysisIntentHandler implements IntentHandler {

    private final BugAnalysisService bugAnalysisService;

    @Override
    public QueryIntent canHandle() {
        return QueryIntent.BUG_ANALYSIS;
    }

    @Override
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request, UUID taskId) {
        return bugAnalysisService.analyzeBugReport(request.query())
                .thenApply(response -> UniversalSyncResponse.from(response, canHandle()));
    }

    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request, UUID taskId) {
        return Mono.fromFuture(() -> bugAnalysisService.analyzeBugReport(request.query()))
                .map(UniversalResponse::from)
                .flux();
    }
}
