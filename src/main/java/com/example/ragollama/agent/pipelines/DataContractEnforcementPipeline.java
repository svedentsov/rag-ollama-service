package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.datacontract.domain.DataContractEnforcerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для проверки DTO на обратную совместимость.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link DataContractEnforcerAgent}. Этот агент использует LLM для
 * семантического сравнения двух версий Java-кода DTO.
 */
@Component
@RequiredArgsConstructor
public class DataContractEnforcementPipeline implements AgentPipeline {

    private final DataContractEnforcerAgent enforcerAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "data-contract-enforcement-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(enforcerAgent)
        );
    }
}
