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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            EvaluationResult currentResult = (EvaluationResult) context.payload().get("evaluationResult");
            // 1. Сохраняем текущий результат
            EvaluationHistory currentHistory = EvaluationHistory.builder()
                    .f1Score(currentResult.retrievalF1Score())
                    .recall(currentResult.retrievalRecall())
                    .precision(currentResult.retrievalPrecision())
                    .meanReciprocalRank(currentResult.meanReciprocalRank())
                    .ndcgAt5(currentResult.ndcgAt5())
                    .totalRecords(currentResult.totalRecords())
                    .triggeringSourceId((String) context.payload().get("triggeringSourceId"))
                    .build();
            historyRepository.save(currentHistory);
            // 2. Получаем последний результат (baseline), не включая только что сохраненный
            // PageRequest.of(1, 1) пропускает первую (самую новую) запись и берет вторую.
            List<EvaluationHistory> lastRuns = historyRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(1, 1));
            if (lastRuns.isEmpty()) {
                return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Это первый прогон оценки, сравнение невозможно.", Map.of());
            }
            EvaluationHistory baseline = lastRuns.get(0);

            // 3. Сравниваем
            double f1Delta = currentResult.retrievalF1Score() - baseline.getF1Score();
            double threshold = evaluationProperties.f1ScoreThreshold();
            String summary;
            if (currentResult.retrievalF1Score() < threshold) {
                summary = String.format("!!! РЕГРЕССИЯ КАЧЕСТВА !!! F1-Score (%.4f) упал ниже порога (%.2f). Дельта с baseline: %.4f",
                        currentResult.retrievalF1Score(), threshold, f1Delta);
                log.error(summary);
            } else {
                summary = String.format("Качество стабильно. F1-Score: %.4f (Дельта с baseline: %.4f)",
                        currentResult.retrievalF1Score(), f1Delta);
                log.info(summary);
            }
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("f1Delta", f1Delta));
        });
    }
}
