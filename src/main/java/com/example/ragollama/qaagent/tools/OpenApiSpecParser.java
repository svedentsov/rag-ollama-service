package com.example.ragollama.qaagent.tools;

import com.example.ragollama.shared.exception.ProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Инфраструктурный сервис, инкапсулирующий логику парсинга OpenAPI спецификаций.
 * <p>
 * Использует библиотеку {@code swagger-parser} для надежного разбора
 * спецификаций из различных источников (URL, текст) в структурированный
 * Java-объект {@link OpenAPI}.
 */
@Slf4j
@Service
public class OpenApiSpecParser {

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
}
