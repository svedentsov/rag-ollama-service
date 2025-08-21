package com.example.ragollama.rag.domain.reranking;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Типобезопасная конфигурация для управления сервисом переранжирования и его стратегиями.
 * <p>
 * Позволяет включать/выключать и настраивать каждую стратегию через {@code application.yml}.
 *
 * @param enabled    Глобальный флаг для включения/выключения всего сервиса переранжирования.
 * @param strategies Конфигурации для отдельных стратегий.
 */
@Validated
@ConfigurationProperties(prefix = "app.reranking")
public record RerankingProperties(
        boolean enabled,
        @NotNull Strategies strategies
) {
    /**
     * Контейнер для настроек всех доступных стратегий.
     *
     * @param keywordBoost Настройки для стратегии повышения ранга по ключевым словам.
     */
    public record Strategies(@NotNull KeywordBoost keywordBoost) {
    }

    /**
     * Настройки для {@link KeywordBoostStrategy}.
     *
     * @param enabled     Включает или выключает эту стратегию.
     * @param boostFactor Коэффициент, на который умножается количество совпавших
     *                    ключевых слов для получения "буста" к оценке релевантности.
     */
    public record KeywordBoost(boolean enabled, double boostFactor) {
    }
}
