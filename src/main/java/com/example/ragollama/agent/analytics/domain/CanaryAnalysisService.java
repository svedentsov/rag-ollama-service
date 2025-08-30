package com.example.ragollama.agent.analytics.domain;

import com.example.ragollama.agent.analytics.api.dto.CanaryAnalysisRequest;
import com.example.ragollama.agent.analytics.model.MetricJudgement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Детерминированный сервис для выполнения статистического анализа метрик.
 * <p>
 * Использует тест Манна-Уитни для сравнения распределений метрик baseline и canary,
 * что позволяет сделать математически обоснованный вывод о наличии или отсутствии
 * статистически значимых различий.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CanaryAnalysisService {

    private static final double SIGNIFICANCE_LEVEL_ALPHA = 0.05;

    /**
     * Результат статистического сравнения.
     */
    public enum StatisticalResult {
        NO_SIGNIFICANT_DIFFERENCE,
        SIGNIFICANTLY_WORSE,
        SIGNIFICANTLY_BETTER,
        INSUFFICIENT_DATA
    }

    /**
     * Выполняет статистический анализ для всех предоставленных метрик.
     *
     * @param metricsData Карта с данными метрик для baseline и canary.
     * @return Список объектов {@link MetricJudgement} с результатами тестов.
     */
    public List<MetricJudgement> performStatisticalAnalysis(Map<String, CanaryAnalysisRequest.MetricData> metricsData) {
        return metricsData.entrySet().stream()
                .map(entry -> {
                    String metricName = entry.getKey();
                    CanaryAnalysisRequest.MetricData data = entry.getValue();
                    return analyzeMetric(metricName, data.baselineValues(), data.canaryValues());
                })
                .collect(Collectors.toList());
    }

    private MetricJudgement analyzeMetric(String metricName, List<Double> baseline, List<Double> canary) {
        if (baseline == null || canary == null || baseline.size() < 5 || canary.size() < 5) {
            return new MetricJudgement(metricName, StatisticalResult.INSUFFICIENT_DATA, -1.0, "Недостаточно данных для анализа.");
        }

        MannWhitneyUTest test = new MannWhitneyUTest();
        double[] baselineArray = baseline.stream().mapToDouble(Double::doubleValue).toArray();
        double[] canaryArray = canary.stream().mapToDouble(Double::doubleValue).toArray();

        double pValue = test.mannWhitneyUTest(baselineArray, canaryArray);
        StatisticalResult result;

        if (pValue > SIGNIFICANCE_LEVEL_ALPHA) {
            result = StatisticalResult.NO_SIGNIFICANT_DIFFERENCE;
        } else {
            double baselineMedian = calculateMedian(baselineArray);
            double canaryMedian = calculateMedian(canaryArray);
            result = (canaryMedian > baselineMedian) ? StatisticalResult.SIGNIFICANTLY_WORSE : StatisticalResult.SIGNIFICANTLY_BETTER;
        }

        log.info("Статистический анализ для '{}': p-value={}, результат={}", metricName, pValue, result);
        return new MetricJudgement(metricName, result, pValue, ""); // Интерпретация будет добавлена LLM
    }

    private double calculateMedian(double[] data) {
        java.util.Arrays.sort(data);
        if (data.length % 2 == 0) {
            return (data[data.length / 2 - 1] + data[data.length / 2]) / 2.0;
        }
        return data[data.length / 2];
    }
}
