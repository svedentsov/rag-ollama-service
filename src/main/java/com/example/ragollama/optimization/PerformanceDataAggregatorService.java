package com.example.ragollama.optimization;

import com.example.ragollama.evaluation.RagEvaluationService;
import com.example.ragollama.evaluation.model.EvaluationResult;
import com.example.ragollama.ingestion.IngestionProperties;
import com.example.ragollama.monitoring.domain.FeedbackLogRepository;
import com.example.ragollama.monitoring.domain.KnowledgeGapRepository;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-агрегатор для сбора данных, адаптированный для R2DBC.
 */
@Service
@RequiredArgsConstructor
public class PerformanceDataAggregatorService {

    private final RagEvaluationService evaluationService;
    private final FeedbackLogRepository feedbackLogRepository;
    private final KnowledgeGapRepository knowledgeGapRepository;
    private final RetrievalProperties retrievalProperties;
    private final IngestionProperties ingestionProperties;

    @Builder
    public record PerformanceSnapshot(
            EvaluationResult evaluationResult,
            List<String> recentNegativeFeedback,
            List<String> recentKnowledgeGaps,
            Map<String, Object> currentConfig
    ) {}

    public CompletableFuture<PerformanceSnapshot> aggregatePerformanceData() {
        Mono<EvaluationResult> evalMono = evaluationService.evaluate();

        Mono<List<String>> feedbackMono = feedbackLogRepository.findAll()
                .filter(fb -> !fb.getIsHelpful())
                .map(fb -> fb.getUserComment() != null ? fb.getUserComment() : "Негативная оценка без комментария.")
                .take(20)
                .collectList();

        Mono<List<String>> gapsMono = knowledgeGapRepository.findAll()
                .map(com.example.ragollama.monitoring.model.KnowledgeGap::getQueryText)
                .take(20)
                .collectList();

        return Mono.zip(evalMono, feedbackMono, gapsMono)
                .map(tuple -> PerformanceSnapshot.builder()
                        .evaluationResult(tuple.getT1())
                        .recentNegativeFeedback(tuple.getT2())
                        .recentKnowledgeGaps(tuple.getT3())
                        .currentConfig(Map.of(
                                "retrieval", retrievalProperties,
                                "ingestion", ingestionProperties
                        ))
                        .build())
                .toFuture();
    }
}
