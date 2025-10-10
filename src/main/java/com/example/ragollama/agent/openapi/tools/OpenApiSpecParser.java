package com.example.ragollama.agent.openapi.tools;

import com.example.ragollama.agent.openapi.api.dto.OpenApiSourceRequest;
import com.example.ragollama.agent.openapi.model.EndpointInfo;
import com.example.ragollama.shared.exception.ProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Инфраструктурный сервис, инкапсулирующий логику парсинга OpenAPI спецификаций.
 * <p>
 * Использует библиотеку {@code swagger-parser} для надежного разбора
 * спецификаций из различных источников (URL, текст) в структурированный
 * Java-объект {@link OpenAPI}. Эта версия адаптирована для работы с
 * полиморфным DTO {@link OpenApiSourceRequest} и включает Javadoc.
 */
@Slf4j
@Service
public class OpenApiSpecParser {

    /**
     * Универсальный метод для парсинга спецификации из полиморфного источника.
     *
     * @param source DTO с источником (URL или контент).
     * @return Распарсенный объект {@link OpenAPI}.
     */
    public OpenAPI parse(OpenApiSourceRequest source) {
        return switch (source) {
            case OpenApiSourceRequest.ByUrl byUrl -> parseFromUrl(byUrl.value());
            case OpenApiSourceRequest.ByContent byContent -> parseFromContent(byContent.value());
        };
    }

    /**
     * Парсит спецификацию из строки.
     *
     * @param specContent Содержимое спецификации в виде YAML или JSON.
     * @return Распарсенный объект {@link OpenAPI}.
     * @throws ProcessingException если парсинг не удался.
     */
    public OpenAPI parseFromContent(String specContent) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true); // Разрешать $ref ссылки
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(specContent, null, options).getOpenAPI();
        if (openAPI == null) {
            log.error("Не удалось распарсить предоставленный OpenAPI контент.");
            throw new ProcessingException("Невалидный OpenAPI контент.");
        }
        return openAPI;
    }

    /**
     * Загружает и парсит спецификацию по URL.
     *
     * @param url URL для загрузки спецификации.
     * @return Распарсенный объект {@link OpenAPI}.
     * @throws ProcessingException если загрузка или парсинг не удались.
     */
    public OpenAPI parseFromUrl(String url) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read(url, null, options);
        if (openAPI == null) {
            log.error("Не удалось загрузить или распарсить OpenAPI спецификацию с URL: {}", url);
            throw new ProcessingException("Не удалось получить OpenAPI спецификацию с URL: " + url);
        }
        return openAPI;
    }

    /**
     * Извлекает все определения эндпоинтов из объекта OpenAPI.
     *
     * @param openApi Распарсенный объект спецификации.
     * @return Список объектов {@link EndpointInfo}.
     */
    public List<EndpointInfo> extractEndpoints(OpenAPI openApi) {
        if (openApi == null || openApi.getPaths() == null) {
            return List.of();
        }
        return openApi.getPaths().entrySet().stream()
                .flatMap(pathEntry -> {
                    String path = pathEntry.getKey();
                    return pathEntry.getValue().readOperationsMap().keySet().stream()
                            .map(httpMethod -> new EndpointInfo(path, HttpMethod.valueOf(httpMethod.name())));
                })
                .toList();
    }

    /**
     * Находит операцию по ее идентификатору и форматирует ее детали в
     * человекочитаемую строку для передачи в LLM.
     *
     * @param openApi            Распарсенный объект спецификации.
     * @param endpointIdentifier Идентификатор в формате "METHOD /path".
     * @return {@link Optional} с отформатированной строкой или пустой, если эндпоинт не найден.
     */
    public Optional<String> formatOperationDetails(OpenAPI openApi, String endpointIdentifier) {
        String[] parts = endpointIdentifier.split("\\s+", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }

        HttpMethod method = HttpMethod.valueOf(parts[0].toUpperCase());
        String path = parts[1];

        return Optional.ofNullable(openApi.getPaths())
                .map(paths -> paths.get(path))
                .flatMap(pathItem -> Optional.ofNullable(pathItem.readOperationsMap().get(PathItem.HttpMethod.valueOf(method.name()))))
                .map(operation -> formatOperation(path, method.name(), operation));
    }

    /**
     * Вспомогательный метод для форматирования деталей операции в текстовый вид.
     *
     * @param path      Путь эндпоинта.
     * @param method    HTTP-метод.
     * @param operation Объект операции из спецификации.
     * @return Строка с описанием.
     */
    private String formatOperation(String path, String method, Operation operation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Endpoint: %s %s\n", method.toUpperCase(), path));
        if (operation.getSummary() != null) {
            sb.append(String.format("Summary: %s\n", operation.getSummary()));
        }
        if (operation.getDescription() != null) {
            sb.append(String.format("Description: %s\n", operation.getDescription()));
        }
        return sb.toString();
    }
}
