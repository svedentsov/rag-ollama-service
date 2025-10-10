package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.datageneration.impl.DataGeneratorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для генерации больших объемов статистически-релевантных
 * синтетических данных.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link DataGeneratorAgent}.
 */
@Component
@RequiredArgsConstructor
public class StatisticalDataGenerationPipeline implements AgentPipeline {

    private final DataGeneratorAgent dataGenerator;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "statistical-data-generation-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(dataGenerator)
        );
    }
}
