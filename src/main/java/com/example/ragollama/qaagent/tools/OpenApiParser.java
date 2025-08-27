package com.example.ragollama.qaagent.tools;

import com.example.ragollama.shared.exception.ProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Утилитарный сервис для парсинга OpenAPI/Swagger спецификаций.
 * <p>
 * Использует библиотеку `swagger-parser` для надежного разбора
 * спецификаций и извлечения детальной информации об эндпоинтах.
 */
@Slf4j
@Service
public class OpenApiParser {

    /**
     * Извлекает детали указанного эндпоинта из спецификации.
     *
     * @param openApi      Распарсенный объект OpenAPI.
     * @param endpointName Имя эндпоинта для анализа (например, "GET /api/v1/users/{id}").
     * @return Человекочитаемое описание эндпоинта для передачи в LLM.
     */
    public String extractEndpointDetails(OpenAPI openApi, String endpointName) {
        String[] parts = endpointName.split("\\s+", 2);
        if (parts.length != 2) {
            return "Неверный формат эндпоинта. Ожидается 'METHOD /path'.";
        }
        String method = parts[0].toUpperCase();
        String path = parts[1];

        PathItem pathItem = openApi.getPaths().get(path);
        if (pathItem == null) {
            return "Детали для эндпоинта '" + endpointName + "' не найдены: путь не существует.";
        }

        Operation operation = pathItem.readOperationsMap().get(PathItem.HttpMethod.valueOf(method));
        if (operation == null) {
            return "Детали для эндпоинта '" + endpointName + "' не найдены: HTTP метод не существует для данного пути.";
        }

        return formatOperation(path, method, operation);
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

    private String formatOperation(String path, String method, Operation operation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Endpoint: %s %s\n", method, path));
        if (operation.getSummary() != null) sb.append(String.format("Summary: %s\n", operation.getSummary()));
        if (operation.getDescription() != null)
            sb.append(String.format("Description: %s\n", operation.getDescription()));

        if (operation.getParameters() != null) {
            sb.append("Parameters:\n");
            for (Parameter param : operation.getParameters()) {
                sb.append(String.format("  - Name: %s, In: %s, Required: %b, Type: %s\n",
                        param.getName(), param.getIn(), param.getRequired(),
                        param.getSchema() != null ? param.getSchema().getType() : "N/A"));
            }
        }

        if (operation.getResponses() != null) {
            sb.append("Responses:\n");
            for (Map.Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
                sb.append(String.format("  - Status: %s, Description: %s\n", entry.getKey(), entry.getValue().getDescription()));
            }
        }
        return sb.toString();
    }
}
