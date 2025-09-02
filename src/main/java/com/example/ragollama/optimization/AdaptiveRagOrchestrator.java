package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.optimization.model.QueryProfile;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.domain.RagService;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
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
    private final RagService ragService;
    private final RetrievalProperties defaultRetrievalProperties;

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
                    // Этот блок действует как "Фабрика Стратегий".
                    return switch (profile.queryType()) {
                        case FACTUAL -> executeFactualStrategy(request, profile);
                        case ANALYTICAL, HOW_TO, CODE_RELATED -> executeDefaultStrategy(request, profile);
                        default -> executeDefaultStrategy(request, profile); // Fallback
                    };
                });
    }

    /**
     * Стратегия для точных, фактологических запросов.
     * Использует более строгие параметры поиска для повышения точности (precision).
     */
    private CompletableFuture<RagQueryResponse> executeFactualStrategy(RagQueryRequest request, QueryProfile profile) {
        log.info("Применение 'Factual' RAG-стратегии для запроса: {}", request.query());
        int topK = defaultRetrievalProperties.hybrid().vectorSearch().topK() - 1; // Меньше документов
        double threshold = defaultRetrievalProperties.hybrid().vectorSearch().similarityThreshold() + 0.05; // Более строгий порог
        final UUID sessionId = getOrCreateSessionId(request.sessionId());

        return ragService.queryAsync(request.query(), Collections.emptyList(), topK, threshold, sessionId)
                .thenApply(ragAnswer -> new RagQueryResponse(ragAnswer.answer(), ragAnswer.sourceCitations(), sessionId));
    }

    /**
     * Стандартная ("сбалансированная") RAG-стратегия.
     */
    private CompletableFuture<RagQueryResponse> executeDefaultStrategy(RagQueryRequest request, QueryProfile profile) {
        log.info("Применение 'Default' RAG-стратегии для запроса: {}", request.query());
        int topK = defaultRetrievalProperties.hybrid().vectorSearch().topK();
        double threshold = defaultRetrievalProperties.hybrid().vectorSearch().similarityThreshold();
        final UUID sessionId = getOrCreateSessionId(request.sessionId());

        return ragService.queryAsync(request.query(), Collections.emptyList(), topK, threshold, sessionId)
                .thenApply(ragAnswer -> new RagQueryResponse(ragAnswer.answer(), ragAnswer.sourceCitations(), sessionId));
    }

    private UUID getOrCreateSessionId(UUID sessionId) {
        return (sessionId != null) ? sessionId : UUID.randomUUID();
    }
}
