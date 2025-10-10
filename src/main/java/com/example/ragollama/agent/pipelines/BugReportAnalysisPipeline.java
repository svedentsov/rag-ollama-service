package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.buganalysis.domain.BugDuplicateDetectorAgent;
import com.example.ragollama.agent.buganalysis.domain.BugReportSummarizerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для полного анализа входящего баг-репорта.
 * <p>
 * Этот конвейер является эталонным примером последовательного выполнения
 * зависимых шагов. Сначала "сырой" текст структурируется, а затем
 * результат этой структуризации используется для более точного поиска дубликатов.
 */
@Component
@RequiredArgsConstructor
public class BugReportAnalysisPipeline implements AgentPipeline {

    private final BugReportSummarizerAgent summarizerAgent;
    private final BugDuplicateDetectorAgent duplicateDetectorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "bug-report-analysis-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет два последовательных этапа, так как поиск дубликатов
     * зависит от результатов структурирования отчета.
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                // Этап 1: Структурировать отчет и обогатить контекст
                List.of(summarizerAgent),
                // Этап 2: Использовать обогащенный контекст для поиска дубликатов
                List.of(duplicateDetectorAgent)
        );
    }
}
