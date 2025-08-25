package com.example.ragollama.qaagent.model;

import java.util.List;

/**
 * DTO для отчета о регрессионных рисках.
 */
public record RegressionRiskReport(
        List<FileRiskProfile> riskProfiles
) {
}
