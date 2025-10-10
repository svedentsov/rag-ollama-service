package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.analytics.domain.ImpactAnalyzerAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import com.example.ragollama.agent.testanalysis.domain.ChecklistBuilderAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для построения иерархического чек-листа.
 * <p>
 * Конвейер демонстрирует параллельный сбор данных с последующей агрегацией:
 * <ol>
 *     <li><b>Этап 1 (Параллельный):</b> Запускаются {@link GitInspectorAgent} и {@link ImpactAnalyzerAgent}.</li>
 *     <li><b>Этап 2 (Последовательный):</b> {@link ChecklistBuilderAgent}
 *     принимает собранные данные и формирует финальный чек-лист.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class ChecklistBuildingPipeline implements AgentPipeline {

    private final GitInspectorAgent gitInspector;
    private final ImpactAnalyzerAgent impactAnalyzer;
    private final ChecklistBuilderAgent checklistBuilder;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "checklist-building-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(gitInspector, impactAnalyzer),
                List.of(checklistBuilder)
        );
    }
}
