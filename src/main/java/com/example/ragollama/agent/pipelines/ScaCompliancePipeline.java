package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.compliance.domain.ScaComplianceAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для анализа состава ПО (SCA - Software Composition Analysis).
 * <p>
 * Этот конвейер является "фасадом" для бизнес-возможности проверки лицензий
 * зависимостей проекта на соответствие заданной политике. На данный момент он
 * состоит из одного шага, но спроектирован для легкого расширения в будущем
 * (например, добавлением шага для сканирования уязвимостей).
 */
@Component
@RequiredArgsConstructor
public class ScaCompliancePipeline implements AgentPipeline {

    private final ScaComplianceAgent scaComplianceAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "sca-compliance-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(scaComplianceAgent)
        );
    }
}
