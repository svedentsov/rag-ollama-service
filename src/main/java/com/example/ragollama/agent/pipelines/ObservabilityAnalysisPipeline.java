package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.ObservabilityDirectorAgent;
import com.example.ragollama.optimization.TraceCollectorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для AI-управляемого анализа данных наблюдаемости.
 */
@Component
@RequiredArgsConstructor
public class ObservabilityAnalysisPipeline implements AgentPipeline {

    private final TraceCollectorAgent traceCollectorAgent;
    private final ObservabilityDirectorAgent observabilityDirectorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "observability-analysis-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет два последовательных этапа:
     * <ol>
     *     <li>Сбор "сырых" данных трассировки.</li>
     *     <li>Их анализ и интерпретация с помощью AI.</li>
     * </ol>
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(traceCollectorAgent),
                List.of(observabilityDirectorAgent)
        );
    }
}