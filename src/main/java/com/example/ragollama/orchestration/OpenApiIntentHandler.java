package com.example.ragollama.orchestration;

import com.example.ragollama.agent.AgentOrchestratorService;
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
 * Обработчик для намерения "OPENAPI_QUERY".
 * <p>
 * Этот обработчик является мостом между универсальным оркестратором и
 * специализированным конвейером для анализа OpenAPI. Он принимает
 * универсальный запрос и делегирует его выполнение `openapi-pipeline`.
 */
@Service
@RequiredArgsConstructor
public class OpenApiIntentHandler implements IntentHandler {

    private final AgentOrchestratorService orchestratorService;

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryIntent canHandle() {
        return QueryIntent.OPENAPI_QUERY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request, UUID taskId) {
        return orchestratorService.invoke("openapi-pipeline", request.toAgentContext())
                .map(results -> {
                    String answer = (String) results.get(0).details().get("answer");
                    return UniversalSyncResponse.from(answer, canHandle());
                })
                .toFuture();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Потоковая обработка для OpenAPI RAG не имеет смысла, так как ответ
     * генерируется целиком. Поэтому мы эмулируем поток из одного элемента.
     */
    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request, UUID taskId) {
        return orchestratorService.invoke("openapi-pipeline", request.toAgentContext())
                .flatMapMany(results -> {
                    String answer = (String) results.get(0).details().get("answer");
                    return Flux.just(
                            new UniversalResponse.Content(answer),
                            new UniversalResponse.Done("Анализ спецификации завершен.")
                    );
                });
    }
}
