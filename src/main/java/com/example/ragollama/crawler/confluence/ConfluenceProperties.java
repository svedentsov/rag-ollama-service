package com.example.ragollama.crawler.confluence;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Типобезопасная конфигурация для интеграции с Confluence.
 * <p>
 * Загружает настройки из {@code application.yml} с префиксом
 * {@code app.integrations.confluence}.
 *
 * @param baseUrl  Базовый URL вашего инстанса Confluence.
 * @param apiUser  Email пользователя для API-аутентификации.
 * @param apiToken API-токен, сгенерированный в Confluence.
 */
@Validated
@ConfigurationProperties(prefix = "app.integrations.confluence")
public record ConfluenceProperties(
        @NotBlank String baseUrl,
        @NotBlank String apiUser,
        @NotBlank String apiToken
) {
}
