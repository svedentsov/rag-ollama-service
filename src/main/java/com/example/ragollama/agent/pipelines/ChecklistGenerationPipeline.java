package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.testanalysis.domain.ChecklistGeneratorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для генерации чек-листа для ручного тестирования.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link ChecklistGeneratorAgent}.
 */
@Component
@RequiredArgsConstructor
public class ChecklistGenerationPipeline implements AgentPipeline {

    private final ChecklistGeneratorAgent checklistGenerator;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "checklist-generation-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(List.of(checklistGenerator));
    }
}
