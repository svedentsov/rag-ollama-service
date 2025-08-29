package com.example.ragollama.qaagent.model;

import java.util.List;

/**
 * DTO для отчета о регрессионных рисках.
 *
 * @param riskProfiles Список профилей риска для каждого файла, отсортированный по убыванию опасности.
 */
public record RegressionRiskReport(
        List<FileRiskProfile> riskProfiles
) {
}
