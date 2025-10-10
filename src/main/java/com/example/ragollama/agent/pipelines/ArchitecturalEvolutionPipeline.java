package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.executive.domain.ArchitecturalHealthGovernorAgent;
import com.example.ragollama.agent.executive.domain.fetchers.DependencyGraphBuilderAgent;
import com.example.ragollama.agent.executive.domain.fetchers.TechDebtScannerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для "AI Chief Architect", анализирующего
 * долгосрочное здоровье архитектуры.
 * <p>
 * Конвейер демонстрирует параллельный сбор данных с последующей агрегацией:
 * <ol>
 *     <li><b>Этап 1 (Параллельный):</b> Запускаются сборщики метрик
 *     {@link DependencyGraphBuilderAgent} и {@link TechDebtScannerAgent}.</li>
 *     <li><b>Этап 2 (Последовательный):</b> {@link ArchitecturalHealthGovernorAgent}
 *     принимает собранные данные и формирует стратегический отчет.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class ArchitecturalEvolutionPipeline implements AgentPipeline {

    private final DependencyGraphBuilderAgent graphBuilder;
    private final TechDebtScannerAgent techDebtScanner;
    private final ArchitecturalHealthGovernorAgent governorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "architectural-evolution-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список этапов с параллельным сбором данных.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(graphBuilder, techDebtScanner),
                List.of(governorAgent)
        );
    }
}
