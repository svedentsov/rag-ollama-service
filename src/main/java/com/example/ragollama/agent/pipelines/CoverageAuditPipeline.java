package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.coverage.domain.CoverageAuditorAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для аудита тестового покрытия измененных файлов.
 * <p>
 * Этот конвейер выполняет два последовательных шага:
 * <ol>
 *     <li>{@link GitInspectorAgent} находит измененные файлы.</li>
 *     <li>{@link CoverageAuditorAgent} сопоставляет этот список с данными
 *     из JaCoCo-отчета для определения рисков.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class CoverageAuditPipeline implements AgentPipeline {

    private final GitInspectorAgent gitInspector;
    private final CoverageAuditorAgent coverageAuditor;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "coverage-audit-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список из двух последовательных этапов.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(gitInspector),
                List.of(coverageAuditor)
        );
    }
}
