package com.example.ragollama.agent.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Типобезопасная конфигурация для интеграции с Git.
 *
 * @param repositoryUrl       URL удаленного Git-репозитория.
 * @param personalAccessToken Персональный токен доступа (PAT) для аутентификации.
 */
@Validated
@ConfigurationProperties(prefix = "app.integrations.git")
public record GitProperties(
        @NotBlank String repositoryUrl,
        @NotBlank String personalAccessToken
) {
}
