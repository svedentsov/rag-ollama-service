package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.architecture.domain.ComponentDependencyExtractorAgent;
import com.example.ragollama.agent.architecture.domain.MermaidDiagramGeneratorAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для генерации диаграммы зависимостей.
 */
@Component
@RequiredArgsConstructor
public class ArchitectureVisualizationPipeline implements AgentPipeline {

    private final GitInspectorAgent gitInspector;
    private final ComponentDependencyExtractorAgent dependencyExtractor;
    private final MermaidDiagramGeneratorAgent diagramGenerator;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "architecture-visualization-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Определяет три последовательных этапа:
     * <ol>
     *   <li>Сбор измененных файлов.</li>
     *   <li>Извлечение зависимостей и построение графа.</li>
     *   <li>Генерация кода диаграммы из графа.</li>
     * </ol>
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(gitInspector),
                List.of(dependencyExtractor),
                List.of(diagramGenerator)
        );
    }
}
