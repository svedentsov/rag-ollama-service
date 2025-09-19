package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.CurationActionAgent;
import com.example.ragollama.optimization.KnowledgeConsistencyGuardianAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для проактивного агента "Хранитель Знаний".
 */
@Component
@RequiredArgsConstructor
public class KnowledgeGuardianPipeline implements AgentPipeline {

    private final KnowledgeConsistencyGuardianAgent guardianAgent;
    private final CurationActionAgent actionAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "knowledge-guardian-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(guardianAgent), // Этап 1: Найти противоречие
                List.of(actionAgent)    // Этап 2: Создать задачу
        );
    }
}