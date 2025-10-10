package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.buganalysis.domain.BugDuplicateDetectorAgent;
import com.example.ragollama.agent.buganalysis.domain.BugReportSummarizerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для помощи мануальному тестировщику в создании баг-репорта.
 * <p>
 * Конвейер выполняет два последовательных шага:
 * <ol>
 *     <li>{@link BugReportSummarizerAgent} структурирует "сырой" текст.</li>
 *     <li>{@link BugDuplicateDetectorAgent} ищет дубликаты на основе
 *     структурированного описания.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class BugReportingAssistancePipeline implements AgentPipeline {

    private final BugReportSummarizerAgent summarizerAgent;
    private final BugDuplicateDetectorAgent duplicateDetectorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "bug-reporting-assistance-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(summarizerAgent),
                List.of(duplicateDetectorAgent)
        );
    }
}
