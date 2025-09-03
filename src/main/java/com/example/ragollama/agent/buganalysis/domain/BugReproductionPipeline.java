package com.example.ragollama.agent.buganalysis.domain;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация составного конвейера для воспроизведения багов.
 * <p>
 * Демонстрирует, как "Стратегия" может состоять из нескольких
 * последовательных шагов (агентов).
 */
@Component
@RequiredArgsConstructor
public class BugReproductionPipeline implements AgentPipeline {

    private final BugReportSummarizerAgent summarizerAgent;
    private final BugReproScriptGeneratorAgent scriptGeneratorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "bug-reproduction-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет два последовательных этапа, так как генерация скрипта
     * зависит от результатов структурирования отчета.
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(summarizerAgent),      // Этап 1: Структурировать отчет
                List.of(scriptGeneratorAgent)  // Этап 2: Сгенерировать скрипт
        );
    }
}
