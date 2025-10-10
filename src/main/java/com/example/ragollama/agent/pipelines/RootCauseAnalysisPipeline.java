package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import com.example.ragollama.agent.testanalysis.domain.RootCauseAnalyzerAgent;
import com.example.ragollama.agent.testanalysis.domain.TestFlakyDetectorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация комплексного конвейера для анализа первопричины (RCA) падений тестов.
 * <p>
 * Этот конвейер является примером сложной оркестрации с параллельным сбором "улик":
 * <ol>
 *     <li><b>Этап 1 (Параллельный):</b> {@link TestFlakyDetectorAgent} находит
 *     упавшие тесты, а {@link GitInspectorAgent} — изменения в коде.</li>
 *     <li><b>Этап 2 (Последовательный):</b> {@link RootCauseAnalyzerAgent}
 *     принимает все собранные данные (падения, diff, логи) и синтезирует
 *     финальный отчет о первопричине.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class RootCauseAnalysisPipeline implements AgentPipeline {

    private final TestFlakyDetectorAgent flakyDetector;
    private final GitInspectorAgent gitInspector;
    private final RootCauseAnalyzerAgent rcaAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "root-cause-analysis-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список этапов с параллельным сбором данных.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(flakyDetector, gitInspector),
                List.of(rcaAgent)
        );
    }
}
