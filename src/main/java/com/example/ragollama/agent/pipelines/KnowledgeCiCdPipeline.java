package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.evaluation.RagEvaluationService;
import com.example.ragollama.evaluation.domain.EvaluationComparatorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class KnowledgeCiCdPipeline implements AgentPipeline {

    private final RagEvaluationService evaluationService;
    private final EvaluationComparatorAgent comparatorAgent;

    @Override
    public String getName() {
        return "knowledge-ci-cd-pipeline";
    }

    /**
     * Этот конвейер имеет кастомную реализацию, так как ему нужно
     * преобразовать сложный объект EvaluationResult в AgentResult.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        QaAgent evaluationStep = new QaAgent() {
            @Override
            public String getName() {
                return "rag-evaluation-runner";
            }

            @Override
            public String getDescription() {
                return "Запускает полный прогон оценки по золотому датасету.";
            }

            @Override
            public boolean canHandle(AgentContext context) {
                return true;
            }

            @Override
            public CompletableFuture<AgentResult> execute(AgentContext context) {
                return evaluationService.evaluate()
                        .map(result -> new AgentResult(
                                getName(),
                                AgentResult.Status.SUCCESS,
                                "Оценка успешно завершена.",
                                Map.of("evaluationResult", result)
                        ))
                        .toFuture();
            }
        };

        return List.of(
                List.of(evaluationStep),
                List.of(comparatorAgent)
        );
    }
}
