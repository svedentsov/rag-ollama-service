package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.executive.domain.LlmCostAnalysisAgent;
import com.example.ragollama.agent.executive.domain.RoiSynthesizerAgent;
import com.example.ragollama.agent.executive.domain.fetchers.CloudCostFetcherAgent;
import com.example.ragollama.agent.executive.domain.fetchers.JiraMetricsFetcherAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для "Финансового Губернатора" (AI CFO), выполняющий
 * комплексный анализ затрат и рентабельности инвестиций (ROI).
 * <p>
 * Этот конвейер является эталонным примером использования параллелизма:
 * <ol>
 *     <li>На первом этапе асинхронно и параллельно запускаются все
 *     агенты-сборщики данных из независимых источников.</li>
 *     <li>На втором, финальном этапе запускается агент-синтезатор, который
 *     агрегирует все собранные данные и формирует стратегический отчет.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class FinancialRoiAnalysisPipeline implements AgentPipeline {

    // Агенты-сборщики для первого этапа
    private final CloudCostFetcherAgent cloudCostFetcherAgent;
    private final LlmCostAnalysisAgent llmCostAnalysisAgent;
    private final JiraMetricsFetcherAgent jiraMetricsFetcherAgent;
    // В реальной системе здесь мог бы быть агент для анализа продуктовых метрик

    // Агент-синтезатор для финального этапа
    private final RoiSynthesizerAgent roiSynthesizerAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "financial-roi-analysis-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список этапов конвейера, оптимизированный для параллельного выполнения.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                // Этап 1: Параллельный сбор всех финансовых данных
                List.of(
                        cloudCostFetcherAgent,
                        llmCostAnalysisAgent,
                        jiraMetricsFetcherAgent
                ),
                // Этап 2: Синтез отчета после сбора всех данных
                List.of(roiSynthesizerAgent)
        );
    }
}
