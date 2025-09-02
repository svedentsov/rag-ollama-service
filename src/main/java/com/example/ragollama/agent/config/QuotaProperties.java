package com.example.ragollama.agent.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * Типобезопасная конфигурация для управления квотами на использование LLM.
 *
 * @param enabled     Включает/выключает систему квот.
 * @param defaultTier Уровень квоты по умолчанию для новых или ненайденных пользователей.
 * @param tiers       Карта с описанием уровней квот.
 * @param costs       Настройки стоимости токенов для FinOps-отчетов.
 */
@Validated
@ConfigurationProperties(prefix = "app.finops.quotas")
public record QuotaProperties(
        boolean enabled,
        @NotBlank String defaultTier,
        @NotNull Map<String, Tier> tiers,
        @NotNull Costs costs
) {
    /**
     * Конфигурация одного уровня (тира) квоты.
     *
     * @param totalTokensLimit Месячный лимит на общее количество токенов (промпт + генерация).
     */
    public record Tier(@Positive long totalTokensLimit) {
    }

    /**
     * Стоимость токенов для расчета в отчетах (в долларах за 1000 токенов).
     *
     * @param input  Стоимость 1000 токенов на входе (prompt).
     * @param output Стоимость 1000 токенов на выходе (completion).
     */
    public record Costs(double input, double output) {
    }
}
