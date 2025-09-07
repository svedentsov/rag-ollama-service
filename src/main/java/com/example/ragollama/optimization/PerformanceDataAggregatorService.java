package com.example.ragollama.optimization;

import com.example.ragollama.evaluation.RagEvaluationService;
import com.example.ragollama.evaluation.model.EvaluationResult;
import com.example.ragollama.ingestion.IngestionProperties;
import com.example.ragollama.monitoring.KnowledgeGapRepository;
import com.example.ragollama.monitoring.domain.FeedbackLogRepository;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Сервис-агрегатор, собирающий все данные, необходимые для работы
 * агента-оптимизатора RAG.
 * <p>
 * Инкапсулирует логику взаимодействия с различными репозиториями и сервисами,
 * предоставляя агенту единый, полный "слепок" текущего состояния и
 * производительности системы.
 */
@Service
@RequiredArgsConstructor
public class PerformanceDataAggregatorService {

    private final RagEvaluationService evaluationService;
    private final FeedbackLogRepository feedbackLogRepository;
    private final KnowledgeGapRepository knowledgeGapRepository;
    private final RetrievalProperties retrievalProperties;
    private final IngestionProperties ingestionProperties;

    /**
     * DTO для передачи собранных данных в агент.
     */
    @Builder
    public record PerformanceSnapshot(
            EvaluationResult evaluationResult,
            List<String> recentNegativeFeedback,
            List<String> recentKnowledgeGaps,
            Map<String, Object> currentConfig
    ) {
    }

    /**
     * Асинхронно собирает полный снимок производительности системы.
     *
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * объект {@link PerformanceSnapshot}.
     */
    public CompletableFuture<PerformanceSnapshot> aggregatePerformanceData() {
        Mono<EvaluationResult> evalMono = evaluationService.evaluate();

        // Асинхронно выполняем все запросы к БД
        CompletableFuture<List<String>> feedbackFuture = CompletableFuture.supplyAsync(() ->
                feedbackLogRepository.findAll(PageRequest.of(0, 20)).stream()
                        .filter(fb -> !fb.getIsHelpful())
                        .map(fb -> fb.getUserComment() != null ? fb.getUserComment() : "Негативная оценка без комментария.")
                        .toList());

        CompletableFuture<List<String>> gapsFuture = CompletableFuture.supplyAsync(() ->
                knowledgeGapRepository.findAll(PageRequest.of(0, 20)).stream()
                        .map(com.example.ragollama.monitoring.model.KnowledgeGap::getQueryText)
                        .toList());

        // Когда все данные собраны, конструируем финальный объект
        return evalMono.toFuture()
                .thenCombine(feedbackFuture, (evalResult, feedback) ->
                        gapsFuture.thenApply(gaps ->
                                PerformanceSnapshot.builder()
                                        .evaluationResult(evalResult)
                                        .recentNegativeFeedback(feedback)
                                        .recentKnowledgeGaps(gaps)
                                        .currentConfig(Map.of(
                                                "retrieval", retrievalProperties,
                                                "ingestion", ingestionProperties
                                        ))
                                        .build()
                        )
                ).thenCompose(Function.identity());
    }
}
