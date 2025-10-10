package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.ux.domain.UserBehaviorSimulatorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для симуляции E2E-сценария пользователя.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link UserBehaviorSimulatorAgent}.
 */
@Component
@RequiredArgsConstructor
public class UserBehaviorSimulationPipeline implements AgentPipeline {

    private final UserBehaviorSimulatorAgent simulatorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "user-behavior-simulation-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(List.of(simulatorAgent));
    }
}
