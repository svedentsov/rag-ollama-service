package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.architecture.domain.ArchConsistencyMapperAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для проверки архитектурной консистентности.
 * <p>
 * Этот конвейер является "фасадом" для бизнес-возможности проверки
 * кода на соответствие заданным архитектурным принципам. В данный момент он
 * состоит из одного шага, но спроектирован для легкого расширения в будущем.
 */
@Component
@RequiredArgsConstructor
public class ArchConsistencyMappingPipeline implements AgentPipeline {

    private final ArchConsistencyMapperAgent archConsistencyMapperAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "arch-consistency-mapping-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(archConsistencyMapperAgent)
        );
    }
}
