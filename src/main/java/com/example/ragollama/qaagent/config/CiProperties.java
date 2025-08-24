package com.example.ragollama.qaagent.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Типобезопасная конфигурация для интеграции с CI/CD системой.
 *
 * @param baseUrl  Базовый URL CI-системы (например, Jenkins).
 * @param username Имя пользователя для аутентификации.
 * @param apiToken API-токен для аутентификации.
 */
@Validated
@ConfigurationProperties(prefix = "app.integrations.ci")
public record CiProperties(
        @NotBlank String baseUrl,
        @NotBlank String username,
        @NotBlank String apiToken
) {
}
