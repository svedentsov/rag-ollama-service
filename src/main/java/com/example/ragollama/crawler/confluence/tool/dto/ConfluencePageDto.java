package com.example.ragollama.crawler.confluence.tool.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO для десериализации ответа от Confluence API при запросе страницы.
 * <p>
 * Содержит только те поля, которые необходимы для работы краулера.
 *
 * @param id       Уникальный идентификатор страницы.
 * @param spaceKey Ключ пространства, которому принадлежит страница.
 * @param title    Заголовок страницы.
 * @param body     Объект, содержащий тело страницы.
 * @param links    Карта со ссылками, включая ссылку на UI.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfluencePageDto(
        String id,
        @JsonProperty("spaceKey") String spaceKey,
        String title,
        Body body,
        @JsonProperty("_links") Links links
) {
    /**
     * Представляет тело страницы.
     *
     * @param storage Контент в формате XHTML.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Storage storage) {
    }

    /**
     * Контейнер для контента.
     *
     * @param value Текст контента.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Storage(String value) {
    }

    /**
     * Контейнер для ссылок.
     *
     * @param webui Ссылка на страницу в веб-интерфейсе.
     * @param next  Ссылка на следующую страницу результатов (для пагинации).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Links(String webui, String next) {
    }

    /**
     * DTO для обработки пагинированных ответов от API Confluence.
     *
     * @param results Список результатов на текущей странице.
     * @param links   Ссылки для навигации по страницам.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaginatedResponse(
            List<ConfluencePageDto> results,
            @JsonProperty("_links") Links links
    ) {
    }
}
