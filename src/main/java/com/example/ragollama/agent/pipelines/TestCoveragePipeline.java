package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.coverage.domain.TestGapAnalyzerAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для анализа пробелов в тестовом покрытии.
 * <p>
 * Этот конвейер демонстрирует последовательное выполнение двух шагов:
 * <ol>
 *     <li>Сначала {@link GitInspectorAgent} находит все измененные файлы.</li>
 *     <li>Затем {@link TestGapAnalyzerAgent} использует этот список для поиска
 *     файлов исходного кода, для которых не были изменены тесты.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class TestCoveragePipeline implements AgentPipeline {

    private final GitInspectorAgent gitInspector;
    private final TestGapAnalyzerAgent testGapAnalyzer;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-coverage-pipeline";
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
                List.of(testGapAnalyzer)
        );
    }
}
