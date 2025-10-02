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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class SummarizationIntentHandler implements IntentHandler {

    private final SummarizationService summarizationService;
    private final ChatIntentHandler chatIntentHandler;

    @Override
    public QueryIntent canHandle() {
        return QueryIntent.SUMMARIZATION;
    }

    @Override
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request, UUID taskId) {
        if (request.context() == null || request.context().isBlank()) {
            return chatIntentHandler.handleSync(request, taskId);
        }
        return summarizationService.summarizeAsync(request.context(), null)
                .map(summary -> UniversalSyncResponse.from(summary, canHandle()))
                .toFuture();
    }

    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request, UUID taskId) {
        if (request.context() == null || request.context().isBlank()) {
            return chatIntentHandler.handleStream(request, taskId);
        }
        return summarizationService.summarizeAsync(request.context(), null)
                .map(UniversalResponse::from)
                .flux();
    }
}
