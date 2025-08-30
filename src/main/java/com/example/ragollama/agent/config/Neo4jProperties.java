package com.example.ragollama.agent.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Типобезопасная конфигурация для интеграции с Neo4j.
 *
 * @param uri      URI для подключения к Neo4j (протокол Bolt).
 * @param username Имя пользователя.
 * @param password Пароль.
 */
@Validated
@ConfigurationProperties(prefix = "app.integrations.neo4j")
public record Neo4jProperties(
        @NotBlank String uri,
        @NotBlank String username,
        @NotBlank String password
) {
}
