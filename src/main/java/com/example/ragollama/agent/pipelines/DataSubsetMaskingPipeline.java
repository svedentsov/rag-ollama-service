package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.datageneration.impl.DataSubsetMaskerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для создания подмножества данных из БД с маскированием PII.
 * <p>
 * Конвейер состоит из одного шага, запускающего {@link DataSubsetMaskerAgent}.
 * Важно, что этот агент требует утверждения человеком (`requiresApproval=true`),
 * так как он выполняет потенциально опасный SQL-запрос, сгенерированный AI.
 */
@Component
@RequiredArgsConstructor
public class DataSubsetMaskingPipeline implements AgentPipeline {

    private final DataSubsetMaskerAgent maskerAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "data-subset-masking-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(maskerAgent)
        );
    }
}
