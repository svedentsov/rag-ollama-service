package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.buganalysis.domain.BugDuplicateDetectorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для обработки события создания нового бага в Jira.
 * <p>
 * Конвейер запускает {@link BugDuplicateDetectorAgent}, чтобы немедленно
 * проверить новый баг на дубликаты и предоставить аналитику команде,
 * сокращая время на ручной триаж.
 */
@Component
@RequiredArgsConstructor
public class JiraBugCreationPipeline implements AgentPipeline {

    private final BugDuplicateDetectorAgent duplicateDetector;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "jira-bug-creation-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(duplicateDetector)
        );
    }
}
