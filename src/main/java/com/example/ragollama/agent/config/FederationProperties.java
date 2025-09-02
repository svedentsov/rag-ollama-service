package com.example.ragollama.agent.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Типобезопасная конфигурация для федеративного анализа.
 * <p>
 * Загружает из {@code application.yml} каталог отслеживаемых проектов,
 * позволяя системе работать в multi-tenant режиме, анализируя метрики
 * из разных источников.
 * <p>
 * <b>Важное архитектурное решение:</b> поле {@code projects} инициализируется
 * пустым списком, чтобы гарантировать, что оно никогда не будет {@code null},
 * даже если соответствующая секция в `application.yml` отсутствует.
 * Это делает сервисы, использующие эту конфигурацию, более отказоустойчивыми.
 *
 * @param projects Список конфигураций для каждого отслеживаемого проекта.
 */
@Validated
@ConfigurationProperties(prefix = "app.federation")
public record FederationProperties(
        List<ProjectConfig> projects
) {
    /**
     * Компактный конструктор для установки значений по умолчанию.
     * Инициализирует список проектов, чтобы избежать NullPointerException.
     */
    public FederationProperties {
        if (projects == null) {
            projects = new ArrayList<>();
        }
    }

    /**
     * Конфигурация одного проекта в рамках федерации.
     *
     * @param id     Уникальный идентификатор проекта. Должен соответствовать
     *               значению `projectId` в таблицах с метриками.
     * @param name   Человекочитаемое имя проекта для отчетов.
     * @param gitUrl URL Git-репозитория (для будущих расширений).
     */
    public record ProjectConfig(
            @NotBlank String id,
            @NotBlank String name,
            @NotBlank String gitUrl
    ) {
    }
}
