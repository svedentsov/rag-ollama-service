package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.testanalysis.domain.TestFlakyDetectorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для обнаружения "плавающих" (flaky) тестов.
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link TestFlakyDetectorAgent}. Этот агент сравнивает два отчета о
 * тестировании для выявления тестов, которые упали в текущем прогоне,
 * но были успешны в эталонном.
 */
@Component
@RequiredArgsConstructor
public class FlakyTestDetectionPipeline implements AgentPipeline {

    private final TestFlakyDetectorAgent flakyDetector;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "flaky-test-detection-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(flakyDetector)
        );
    }
}
