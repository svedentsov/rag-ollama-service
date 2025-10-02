package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.optimization.model.QueryProfile;
import com.example.ragollama.orchestration.RagApplicationService;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveRagOrchestrator {

    private final QueryProfilerAgent profilerAgent;
    private final RagApplicationService ragApplicationService;

    public CompletableFuture<RagQueryResponse> processAdaptive(RagQueryRequest request, UUID taskId) {
        return profilerAgent.execute(new AgentContext(Map.of("query", request.query())))
                .thenCompose(profilerResult -> {
                    QueryProfile profile = (QueryProfile) profilerResult.details().get("queryProfile");
                    RagQueryRequest adjustedRequest = adjustRequestBasedOnProfile(request, profile);
                    return ragApplicationService.processRagRequestAsync(adjustedRequest, taskId);
                });
    }

    private RagQueryRequest adjustRequestBasedOnProfile(RagQueryRequest originalRequest, QueryProfile profile) {
        if (profile.queryType() == QueryProfile.QueryType.FACTUAL && profile.searchScope() == QueryProfile.SearchScope.NARROW) {
            log.info("Применение 'Factual' RAG-стратегии для запроса: {}", originalRequest.query());
            return new RagQueryRequest(
                    originalRequest.query(),
                    originalRequest.sessionId(),
                    Math.max(1, originalRequest.topK() - 2),
                    Math.min(1.0, originalRequest.similarityThreshold() + 0.05)
            );
        }

        log.info("Применение 'Default' RAG-стратегии для запроса: {}", originalRequest.query());
        return originalRequest;
    }
}
