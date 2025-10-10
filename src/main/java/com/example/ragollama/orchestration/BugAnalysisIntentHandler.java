package com.example.ragollama.orchestration;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.buganalysis.mappers.BugAnalysisMapper;
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

/**
 * Обработчик для намерения "BUG_ANALYSIS".
 * <p>
 * Эта версия корректно использует `AgentOrchestratorService` для запуска
 * конвейера и `BugAnalysisMapper` для преобразования результата в публичный DTO.
 */
@Service
@RequiredArgsConstructor
public class BugAnalysisIntentHandler implements IntentHandler {

    private final AgentOrchestratorService orchestratorService;
    private final BugAnalysisMapper bugAnalysisMapper;

    @Override
    public QueryIntent canHandle() {
        return QueryIntent.BUG_ANALYSIS;
    }

    @Override
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request, UUID taskId) {
        return orchestratorService.invoke("bug-report-analysis-pipeline", request.toAgentContext())
                .map(bugAnalysisMapper::toReport)
                .map(report -> UniversalSyncResponse.from(report, canHandle()))
                .toFuture();
    }

    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request, UUID taskId) {
        // Потоковая обработка для анализа бага не имеет смысла, эмулируем поток из одного элемента.
        return Mono.fromFuture(() -> handleSync(request, taskId))
                .map(UniversalResponse::from)
                .flux();
    }
}
