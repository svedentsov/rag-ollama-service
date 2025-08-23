package com.example.ragollama.crawler.api;

import com.example.ragollama.crawler.confluence.ConfluenceCrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для управления фоновыми задачами краулинга.
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
     * Эндпоинт немедленно возвращает ответ `202 Accepted`, подтверждая, что
     * задача принята в обработку. Сам процесс краулинга выполняется в фоновом потоке.
     *
     * @param spaceKey Ключ пространства в Confluence (например, "DEV").
     * @return {@link ResponseEntity} со статусом 202, если задача успешно запущена,
     * или 409 (Conflict), если задача для этого пространства уже выполняется.
     */
    @PostMapping("/confluence/spaces/{spaceKey}")
    @Operation(summary = "Запустить краулинг пространства Confluence",
            description = "Асинхронно запускает полную индексацию всех страниц в указанном пространстве.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Задача краулинга успешно принята в обработку."),
                    @ApiResponse(responseCode = "409", description = "Задача для этого пространства уже выполняется.")
            })
    public ResponseEntity<Void> crawlConfluenceSpace(@PathVariable String spaceKey) {
        boolean started = crawlerService.crawlSpaceAsync(spaceKey);
        return started
                ? ResponseEntity.accepted().build()
                : ResponseEntity.status(409).build();
    }
}
