package com.example.ragollama.agent.executive.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от "Engineering Velocity Governor".
 *
 * @param summary         Ключевой вывод о состоянии инженерных процессов.
 * @param bottlenecks     Приоритизированный список обнаруженных "узких мест".
 * @param recommendations Предложения по устранению проблем.
 */
@Schema(description = "Отчет о производительности инженерных процессов")
@JsonIgnoreProperties(ignoreUnknown = true)
public record EngineeringEfficiencyReport(
        String summary,
        List<ProcessBottleneck> bottlenecks,
        List<String> recommendations
) {
    /**
     * DTO для описания одного "узкого места" в процессе.
     *
     * @param area        Область проблемы (например, "CI/CD Pipeline").
     * @param description Детальное описание проблемы.
     * @param impact      Бизнес-влияние проблемы.
     */
    @Schema(description = "Одно обнаруженное 'узкое место' в процессе разработки")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProcessBottleneck(
            String area,
            String description,
            String impact
    ) {
    }
}
