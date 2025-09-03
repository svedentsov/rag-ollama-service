package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.InteractionAnalyzerAgent;
import com.example.ragollama.optimization.PromptRefinementAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SelfImprovementPipeline implements AgentPipeline {

    private final InteractionAnalyzerAgent analyzerAgent;
    private final PromptRefinementAgent refinementAgent;

    @Override
    public String getName() {
        return "self-improvement-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет два последовательных этапа: сначала анализ логов
     * для поиска неэффективности, затем генерация предложений по
     * улучшению промптов на основе этого анализа.
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(analyzerAgent),
                List.of(refinementAgent)
        );
    }
}
