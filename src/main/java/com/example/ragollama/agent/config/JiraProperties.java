package com.example.ragollama.agent.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Типобезопасная конфигурация для интеграции с Jira.
 *
 * @param baseUrl  Базовый URL вашего инстанса Jira.
 * @param apiUser  Email пользователя для API-аутентификации.
 * @param apiToken API токен, сгенерированный в Jira.
 */
@Validated
@ConfigurationProperties(prefix = "app.integrations.jira")
public record JiraProperties(
        @NotBlank String baseUrl,
        @NotBlank String apiUser,
        @NotBlank String apiToken
) {
}
