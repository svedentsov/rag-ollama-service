package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.analytics.domain.CodeQualityImpactEstimatorAgent;
import com.example.ragollama.agent.analytics.domain.ReleaseReadinessAssessorAgent;
import com.example.ragollama.agent.coverage.domain.CoverageAuditorAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import com.example.ragollama.agent.testanalysis.domain.FlakinessTrackerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация комплексного конвейера для оценки готовности релиза.
 * <p>
 * Этот конвейер является эталоном сложной оркестрации и демонстрирует
 * как последовательное, так и параллельное выполнение агентов:
 * <ol>
 *     <li><b>Этап 1 (Последовательный):</b> {@link GitInspectorAgent} получает список
 *     измененных файлов.</li>
 *     <li><b>Этап 2 (Параллельный):</b> Несколько независимых анализаторов
 *     (покрытие, качество, стабильность) запускаются одновременно, каждый
 *     используя список файлов из первого этапа.</li>
 *     <li><b>Этап 3 (Последовательный):</b> Финальный агент-агрегатор
 *     {@link ReleaseReadinessAssessorAgent} собирает результаты всех
 *     анализаторов и выносит итоговый вердикт.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class ReleaseReadinessPipeline implements AgentPipeline {

    private final GitInspectorAgent gitInspector;
    private final CoverageAuditorAgent coverageAuditor;
    private final CodeQualityImpactEstimatorAgent qualityEstimator;
    private final FlakinessTrackerAgent flakinessTracker;
    private final ReleaseReadinessAssessorAgent assessorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "release-readiness-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список этапов, демонстрирующий параллельное выполнение.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                // Этап 1: Получить контекст (измененные файлы)
                List.of(gitInspector),
                // Этап 2: Запустить все независимые анализаторы параллельно
                List.of(
                        coverageAuditor,
                        qualityEstimator,
                        flakinessTracker
                ),
                // Этап 3: Собрать все результаты и вынести финальный вердикт
                List.of(assessorAgent)
        );
    }
}
