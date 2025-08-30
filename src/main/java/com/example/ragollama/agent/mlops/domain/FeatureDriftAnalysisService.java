package com.example.ragollama.agent.mlops.domain;

import com.example.ragollama.agent.mlops.model.DriftAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Детерминированный сервис для выполнения статистического анализа дрейфа признаков.
 */
@Slf4j
@Service
public class FeatureDriftAnalysisService {

    private static final double EPSILON = 1e-10; // Для предотвращения деления на ноль

    /**
     * Анализирует два набора данных и вычисляет метрики дрейфа для каждого признака.
     *
     * @param baselineData   Эталонный набор данных.
     * @param productionData Текущий набор данных.
     * @return Список результатов анализа для каждого общего признака.
     */
    public List<DriftAnalysisResult> analyze(List<Map<String, Object>> baselineData, List<Map<String, Object>> productionData) {
        if (baselineData.isEmpty() || productionData.isEmpty()) {
            return List.of();
        }

        // Определяем общие признаки для анализа
        var commonFeatures = baselineData.get(0).keySet();
        commonFeatures.retainAll(productionData.get(0).keySet());

        return commonFeatures.stream()
                .map(feature -> calculatePsiForFeature(feature, baselineData, productionData))
                .collect(Collectors.toList());
    }

    private DriftAnalysisResult calculatePsiForFeature(String feature, List<Map<String, Object>> baseline, List<Map<String, Object>> production) {
        // ... Здесь должна быть сложная логика биннинга для числовых данных ...
        // Для простоты, мы реализуем PSI только для категориальных данных.

        Map<Object, Long> baselineCounts = baseline.stream()
                .collect(Collectors.groupingBy(row -> row.get(feature), Collectors.counting()));
        Map<Object, Long> productionCounts = production.stream()
                .collect(Collectors.groupingBy(row -> row.get(feature), Collectors.counting()));

        long baselineTotal = baseline.size();
        long productionTotal = production.size();

        double psi = baselineCounts.keySet().stream()
                .mapToDouble(key -> {
                    double baselinePerc = (baselineCounts.getOrDefault(key, 0L) + EPSILON) / baselineTotal;
                    double productionPerc = (productionCounts.getOrDefault(key, 0L) + EPSILON) / productionTotal;
                    return (baselinePerc - productionPerc) * Math.log(baselinePerc / productionPerc);
                })
                .sum();

        DriftAnalysisResult.DriftLevel level;
        if (psi >= 0.25) {
            level = DriftAnalysisResult.DriftLevel.SEVERE_DRIFT;
        } else if (psi >= 0.1) {
            level = DriftAnalysisResult.DriftLevel.MODERATE_DRIFT;
        } else {
            level = DriftAnalysisResult.DriftLevel.NO_DRIFT;
        }

        return new DriftAnalysisResult(feature, psi, level, String.format("PSI score calculated based on %d categories.", baselineCounts.size()));
    }
}
