package com.example.ragollama.evaluation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.evaluation.scheduler.enabled", havingValue = "true")
public class EvaluationScheduler {

    private final RagEvaluationService evaluationService;
    private final EvaluationProperties evaluationProperties;

    /**
     * Периодически запускает полный прогон оценки RAG-системы по "золотому датасету".
     */
    @Scheduled(cron = "${app.evaluation.scheduler.cron}")
    public void runScheduledEvaluation() {
        log.info("Планировщик запускает автоматическую оценку RAG-системы...");
        evaluationService.evaluate()
                .doOnSuccess(result -> {
                    double threshold = evaluationProperties.f1ScoreThreshold();
                    log.info("Автоматическая оценка завершена. F1-Score: {:.4f} (Порог: {})", result.retrievalF1Score(), threshold);
                    if (result.retrievalF1Score() < threshold) {
                        log.error("""
                                
                                !!! ВНИМАНИЕ: КАЧЕСТВО RAG-СИСТЕМЫ ДЕГРАДИРОВАЛО !!!
                                F1-Score ({}) ниже установленного порога ({}).
                                Требуется анализ результатов и 'золотого датасета'.
                                """, String.format("%.4f", result.retrievalF1Score()), threshold);
                    }
                })
                .doOnError(error -> log.error("Ошибка во время выполнения плановой оценки.", error))
                .subscribe();
    }
}
