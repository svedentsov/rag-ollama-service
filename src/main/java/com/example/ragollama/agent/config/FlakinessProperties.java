package com.example.ragollama.agent.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Типобезопасная конфигурация для агента отслеживания нестабильных тестов.
 * <p>
 * Загружает настройки из {@code application.yml} с префиксом {@code app.analysis.flakiness}.
 * Позволяет гибко настраивать критерии, по которым тест считается "плавающим".
 *
 * @param failureRateThreshold Процент падений (от 0 до 100), выше которого тест
 *                             считается нестабильным.
 * @param minRunsThreshold     Минимальное количество запусков теста за анализируемый
 *                             период, необходимое для того, чтобы он попал в отчет.
 *                             Это предотвращает ложные срабатывания для новых тестов,
 *                             которые могли упасть один раз из одного запуска.
 */
@Validated
@ConfigurationProperties(prefix = "app.analysis.flakiness")
public record FlakinessProperties(
        @Min(1) @Max(100) double failureRateThreshold,
        @Min(2) long minRunsThreshold
) {
}
