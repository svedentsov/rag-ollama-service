package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.autonomy.AutonomousMaintenanceAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для проактивного агента-монитора здоровья.
 * <p>
 * Эта версия была упрощена. Теперь конвейер состоит из одного шага,
 * так как `AutonomousMaintenanceAgent` сам инкапсулирует логику сбора
 * необходимых ему данных через `ProjectHealthAggregatorService`.
 */
@Component
@RequiredArgsConstructor
public class HealthMonitorPipeline implements AgentPipeline {

    private final AutonomousMaintenanceAgent maintenanceAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "health-monitor-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(maintenanceAgent)
        );
    }
}