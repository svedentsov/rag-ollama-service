package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.datageneration.impl.SyntheticDataBuilderAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для генерации синтетических (моковых) данных.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link SyntheticDataBuilderAgent}. Этот агент использует LLM для
 * создания реалистичных JSON-объектов на основе определения Java-класса.
 */
@Component
@RequiredArgsConstructor
public class SyntheticDataGenerationPipeline implements AgentPipeline {

    private final SyntheticDataBuilderAgent dataBuilder;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "synthetic-data-generation-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(dataBuilder)
        );
    }
}
