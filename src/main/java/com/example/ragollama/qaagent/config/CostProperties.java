package com.example.ragollama.qaagent.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Типобезопасная конфигурация для экономической модели оценки стоимости.
 * <p>
 * Загружает бизнес-предположения из {@code application.yml} с префиксом {@code app.analysis.cost-model}.
 *
 * @param perDevHour       Стоимость одного часа работы разработчика.
 * @param perSupportTicket Стоимость обработки одного обращения в техподдержку.
 * @param perLostUser      Средние потери от оттока одного пользователя.
 */
@Validated
@ConfigurationProperties(prefix = "app.analysis.cost-model")
public record CostProperties(
        @Positive double perDevHour,
        @Positive double perSupportTicket,
        @Positive double perLostUser
) {
}
