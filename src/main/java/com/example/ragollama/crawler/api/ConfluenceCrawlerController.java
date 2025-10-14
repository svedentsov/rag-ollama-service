package com.example.ragollama.crawler.api;

import com.example.ragollama.crawler.confluence.ConfluenceCrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Контроллер для управления фоновыми задачами краулинга и индексации.
 * <p>
 * Предоставляет API для асинхронного запуска процесса индексации
 * внешних источников знаний, таких как Confluence.
 */
@RestController
@RequestMapping("/api/v1/crawlers")
@RequiredArgsConstructor
@Tag(name = "Crawlers API", description = "API для запуска фоновой индексации внешних источников")
public class ConfluenceCrawlerController {

    private final ConfluenceCrawlerService crawlerService;

    /**
     * Запускает асинхронную задачу краулинга для указанного пространства Confluence.
     * <p>
     * Эндпоинт немедленно возвращает ответ, а сам процесс краулинга выполняется
     * в фоновом режиме. Метод возвращает {@code Mono<ResponseEntity<Void>>}, что
     * является идиоматичным для WebFlux.
     *
     * @param spaceKey Ключ пространства в Confluence (например, "DEV").
     * @param category Категория, которая будет присвоена всем проиндексированным
     *                 документам из этого пространства (например, "onboarding").
     * @return {@link Mono} с {@link ResponseEntity}, содержащим статус 202, если задача успешно запущена,
     * или 409 (Conflict), если задача для этого пространства уже выполняется.
     */
    @PostMapping("/confluence/spaces/{spaceKey}/index")
    @Operation(summary = "Запустить индексацию пространства Confluence",
            description = "Асинхронно запускает полную индексацию всех страниц в указанном пространстве, " +
                    "присваивая им заданную категорию в метаданных.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Задача индексации успешно принята в обработку."),
                    @ApiResponse(responseCode = "409", description = "Задача для этого пространства уже выполняется.")})
    public Mono<ResponseEntity<Void>> crawlConfluenceSpace(
            @Parameter(description = "Ключ пространства Confluence", example = "ONBOARDING")
            @PathVariable String spaceKey,
            @Parameter(description = "Категория для метаданных документов", example = "onboarding_materials")
            @RequestParam(defaultValue = "general") String category) {
        return crawlerService.crawlSpaceAsync(spaceKey, category)
                .map(started -> started
                        ? ResponseEntity.accepted().<Void>build()
                        : ResponseEntity.status(409).<Void>build()
                );
    }
}
