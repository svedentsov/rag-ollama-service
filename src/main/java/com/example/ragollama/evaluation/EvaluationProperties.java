package com.example.ragollama.evaluation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.evaluation")
public record EvaluationProperties(
        Scheduler scheduler,
        double f1ScoreThreshold
) {
    public record Scheduler(
            boolean enabled,
            String cron
    ) {
    }
}
