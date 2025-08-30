package com.example.ragollama.agent.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Типобезопасная конфигурация для федеративного анализа.
 * <p>
 * Загружает из {@code application.yml} каталог отслеживаемых проектов.
 *
 * @param projects Список конфигураций для каждого проекта.
 */
@Validated
@ConfigurationProperties(prefix = "app.federation")
public record FederationProperties(
        List<ProjectConfig> projects
) {
    /**
     * Конфигурация одного проекта.
     *
     * @param id     Уникальный идентификатор проекта.
     * @param name   Человекочитаемое имя.
     * @param gitUrl URL Git-репозитория.
     */
    public record ProjectConfig(
            @NotBlank String id,
            @NotBlank String name,
            @NotBlank String gitUrl
    ) {
    }
}
