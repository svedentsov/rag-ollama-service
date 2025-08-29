package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от AI On-Call Engineer.
 *
 * @param summary           Краткое резюме инцидента.
 * @param probableCause     Наиболее вероятная причина, определенная AI.
 * @param recentDeployments Список недавних коммитов, которые могут быть связаны с инцидентом.
 * @param recommendations   Предлагаемые немедленные действия.
 */
@Schema(description = "Сводный отчет по инциденту в продакшене")
@JsonIgnoreProperties(ignoreUnknown = true)
public record IncidentReport(
        String summary,
        String probableCause,
        List<String> recentDeployments,
        List<String> recommendations
) {
}
