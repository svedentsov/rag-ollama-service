package com.example.ragollama.agent.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Типобезопасная конфигурация для определения "Доменов Знаний".
 * <p>
 * Загружает из {@code application.yml} каталог доступных доменов,
 * которые используются {@link com.example.ragollama.optimization.KnowledgeRouterAgent}
 * для маршрутизации запросов.
 *
 * @param domains Список сконфигурированных доменов.
 */
@Validated
@ConfigurationProperties(prefix = "app.knowledge-domains")
public record KnowledgeDomainProperties(
        List<Domain> domains
) {
    /**
     * Конфигурация одного домена знаний.
     *
     * @param name        Уникальное, машиночитаемое имя домена.
     *                    Используется для фильтрации по `metadata.doc_category`.
     * @param description Человекочитаемое описание, которое помогает LLM
     *                    понять содержимое домена.
     */
    public record Domain(
            @NotBlank String name,
            @NotBlank String description
    ) {
    }
}
