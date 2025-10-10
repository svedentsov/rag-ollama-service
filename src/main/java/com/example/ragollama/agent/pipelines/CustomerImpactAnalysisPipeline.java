package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.analytics.domain.CustomerImpactAnalyzerAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для анализа влияния изменений на конечных пользователей.
 * <p>
 * Этот конвейер выполняет два последовательных шага:
 * <ol>
 *     <li>Сначала {@link GitInspectorAgent} находит все измененные файлы.</li>
 *     <li>Затем {@link CustomerImpactAnalyzerAgent} использует этот список
 *     для анализа и прогнозирования влияния на UX, API и бизнес-логику.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class CustomerImpactAnalysisPipeline implements AgentPipeline {

    private final GitInspectorAgent gitInspector;
    private final CustomerImpactAnalyzerAgent impactAnalyzer;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "customer-impact-analysis-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список из двух последовательных этапов.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(gitInspector),
                List.of(impactAnalyzer)
        );
    }
}
