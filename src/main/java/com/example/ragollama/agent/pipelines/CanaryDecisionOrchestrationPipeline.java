package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.analytics.domain.CanaryAnalyzerAgent;
import com.example.ragollama.agent.analytics.domain.CanaryDecisionOrchestratorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для принятия и выполнения решения по Canary-развертыванию.
 * <p>
 * Этот конвейер демонстрирует, как один агент может использовать результаты
 * другого для принятия решений:
 * <ol>
 *     <li>{@link CanaryAnalyzerAgent} анализирует метрики и формирует отчет.</li>
 *     <li>{@link CanaryDecisionOrchestratorAgent} принимает этот отчет,
 *     анализирует его в соответствии с политикой и генерирует план действий
 *     (например, запуск CI/CD задачи на откат или продвижение релиза).</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class CanaryDecisionOrchestrationPipeline implements AgentPipeline {

    private final CanaryAnalyzerAgent canaryAnalyzer;
    private final CanaryDecisionOrchestratorAgent orchestratorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "canary-decision-orchestration-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список из двух последовательных этапов.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(canaryAnalyzer),
                List.of(orchestratorAgent)
        );
    }
}
