package com.example.ragollama.evaluation.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.evaluation.EvaluationProperties;
import com.example.ragollama.evaluation.model.EvaluationHistory;
import com.example.ragollama.evaluation.model.EvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Агент-компаратор, адаптированный для R2DBC.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationComparatorAgent implements ToolAgent {

    private final EvaluationHistoryRepository historyRepository;
    private final EvaluationProperties evaluationProperties;

    @Override
    public String getName() {
        return "evaluation-comparator-agent";
    }

    @Override
    public String getDescription() {
        return "Сравнивает текущий результат оценки с baseline и обнаруживает регрессии.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().get("evaluationResult") instanceof EvaluationResult;
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        EvaluationResult currentResult = (EvaluationResult) context.payload().get("evaluationResult");

        EvaluationHistory currentHistory = EvaluationHistory.builder()
                .f1Score(currentResult.retrievalF1Score())
                .recall(currentResult.retrievalRecall())
                .precision(currentResult.retrievalPrecision())
                .meanReciprocalRank(currentResult.meanReciprocalRank())
                .ndcgAt5(currentResult.ndcgAt5())
                .totalRecords(currentResult.totalRecords())
                .triggeringSourceId((String) context.payload().get("triggeringSourceId"))
                .build();

        return historyRepository.save(currentHistory)
                .then(historyRepository.findAllBy(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .collectList())
                .map(lastRuns -> {
                    if (lastRuns.size() < 2) {
                        return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Это первый прогон оценки, сравнение невозможно.", Map.of());
                    }
                    EvaluationHistory baseline = lastRuns.get(1);

                    double f1Delta = currentResult.retrievalF1Score() - baseline.getF1Score();
                    double threshold = evaluationProperties.f1ScoreThreshold();
                    String summary;
                    if (currentResult.retrievalF1Score() < threshold) {
                        summary = String.format("!!! РЕГРЕССИЯ КАЧЕСТВА !!! F1-Score (%.4f) упал ниже порога (%.2f). Дельта: %.4f",
                                currentResult.retrievalF1Score(), threshold, f1Delta);
                        log.error(summary);
                    } else {
                        summary = String.format("Качество стабильно. F1-Score: %.4f (Дельта: %.4f)",
                                currentResult.retrievalF1Score(), f1Delta);
                        log.info(summary);
                    }
                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("f1Delta", f1Delta));
                });
    }
}
