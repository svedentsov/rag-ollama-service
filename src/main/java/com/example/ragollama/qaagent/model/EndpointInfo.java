package com.example.ragollama.qaagent.model;

import org.springframework.http.HttpMethod;

/**
 * Унифицированное представление одного HTTP-эндпоинта.
 * <p>
 * Используется для сравнения эндпоинтов, извлеченных как из
 * OpenAPI спецификации, так и из кода приложения.
 *
 * @param path   Путь эндпоинта (например, "/api/v1/users/{id}").
 * @param method HTTP-метод (GET, POST, и т.д.).
 */
public record EndpointInfo(
        String path,
        HttpMethod method
) {
}
