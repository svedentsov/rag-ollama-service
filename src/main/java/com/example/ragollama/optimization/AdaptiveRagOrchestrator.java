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
import java.util.concurrent.CompletableFuture;

/**
 * Мета-агент (оркестратор), реализующий адаптивную RAG-стратегию.
 * <p>
 * Он динамически выбирает и настраивает RAG-конвейер на основе
 * семантического профиля входящего запроса, который получает от
 * {@link QueryProfilerAgent}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveRagOrchestrator {

    private final QueryProfilerAgent profilerAgent;
    private final RagApplicationService ragApplicationService;

    /**
     * Асинхронно обрабатывает RAG-запрос, применяя адаптивную стратегию.
     *
     * @param request Исходный запрос от пользователя.
     * @return {@link CompletableFuture} с ответом.
     */
    public CompletableFuture<RagQueryResponse> processAdaptive(RagQueryRequest request) {
        // Шаг 1: Профилируем запрос, чтобы понять его природу.
        return profilerAgent.execute(new AgentContext(Map.of("query", request.query())))
                .thenCompose(profilerResult -> {
                    QueryProfile profile = (QueryProfile) profilerResult.details().get("queryProfile");

                    // Шаг 2: На основе профиля, выбираем и настраиваем стратегию.
                    RagQueryRequest adjustedRequest = adjustRequestBasedOnProfile(request, profile);
                    return ragApplicationService.processRagRequestAsync(adjustedRequest);
                });
    }

    /**
     * Корректирует параметры RAG-запроса на основе профиля.
     *
     * @param originalRequest Исходный запрос.
     * @param profile         Семантический профиль запроса.
     * @return Новый, скорректированный запрос.
     */
    private RagQueryRequest adjustRequestBasedOnProfile(RagQueryRequest originalRequest, QueryProfile profile) {
        if (profile.queryType() == QueryProfile.QueryType.FACTUAL && profile.searchScope() == QueryProfile.SearchScope.NARROW) {
            log.info("Применение 'Factual' RAG-стратегии для запроса: {}", originalRequest.query());
            // Для точных запросов уменьшаем количество документов и повышаем порог
            return new RagQueryRequest(
                    originalRequest.query(),
                    originalRequest.sessionId(),
                    Math.max(1, originalRequest.topK() - 2),
                    Math.min(1.0, originalRequest.similarityThreshold() + 0.05)
            );
        }

        log.info("Применение 'Default' RAG-стратегии для запроса: {}", originalRequest.query());
        return originalRequest; // Возвращаем исходный запрос для всех остальных случаев
    }
}
