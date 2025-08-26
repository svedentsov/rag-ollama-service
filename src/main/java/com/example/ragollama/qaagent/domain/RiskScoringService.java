package com.example.ragollama.qaagent.domain;

import com.example.ragollama.qaagent.model.CustomerImpactAnalysis;
import com.example.ragollama.qaagent.model.FileQualityImpact;
import org.springframework.stereotype.Service;

/**
 * Детерминированный сервис для вычисления числовых оценок риска.
 * <p>
 * Инкапсулирует бизнес-логику преобразования качественных оценок
 * от AI-агентов в количественные показатели для построения матрицы рисков.
 */
@Service
public class RiskScoringService {

    /**
     * Вычисляет оценку ВЕРОЯТНОСТИ (Likelihood) возникновения дефекта.
     *
     * @param qualityImpact Результат анализа качества кода.
     * @return Оценка от 1 (низкая) до 5 (высокая).
     */
    public int calculateLikelihood(FileQualityImpact qualityImpact) {
        if (qualityImpact == null) return 1;

        // Простое взвешенное среднее. В реальной системе здесь может быть более сложная модель.
        double score = qualityImpact.defectProbability() * 0.6 + qualityImpact.maintainabilityRisk() * 0.4;
        return (int) Math.round(score / 2.0); // Приводим к шкале 1-5
    }

    /**
     * Вычисляет оценку ВЛИЯНИЯ (Impact) дефекта на бизнес.
     *
     * @param customerImpact Результат анализа влияния на пользователя.
     * @return Оценка от 1 (низкая) до 5 (критическая).
     */
    public int calculateImpact(CustomerImpactAnalysis customerImpact) {
        if (customerImpact == null) return 1;

        return switch (customerImpact.severity().toLowerCase()) {
            case "high" -> 5; // Breaking change
            case "medium" -> 3;
            case "low" -> 2;
            default -> 1;
        };
    }
}
