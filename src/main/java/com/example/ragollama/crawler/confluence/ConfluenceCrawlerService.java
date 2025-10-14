package com.example.ragollama.crawler.confluence;

import com.example.ragollama.crawler.confluence.tool.ConfluenceApiClient;
import com.example.ragollama.crawler.confluence.tool.dto.ConfluencePageDto;
import com.example.ragollama.indexing.IndexingPipelineService;
import com.example.ragollama.indexing.IndexingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сервис-оркестратор для выполнения краулинга пространств Confluence.
 * <p>
 * Отвечает за обход дерева страниц, извлечение контента и его передачу
 * в унифицированный сервис индексации {@link IndexingPipelineService}.
 * Реализует простую блокировку для предотвращения одновременного запуска
 * нескольких задач для одного и того же пространства.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceCrawlerService {

    private final ConfluenceApiClient confluenceApiClient;
    private final IndexingPipelineService indexingPipelineService;
    private final Map<String, AtomicBoolean> spaceLocks = new ConcurrentHashMap<>();

    /**
     * Асинхронно запускает полный краулинг указанного пространства Confluence.
     * <p>
     * Метод возвращает {@link Mono}, которое при подписке запускает весь процесс
     * в фоновом режиме на планировщике {@code Schedulers.boundedElastic()}.
     *
     * @param spaceKey Ключ пространства для краулинга.
     * @param category Категория для присвоения документам.
     * @return {@code Mono<Boolean>}, которое эммитит {@code true}, если задача была успешно запущена,
     * или {@code false}, если задача для этого пространства уже выполняется.
     */
    public Mono<Boolean> crawlSpaceAsync(String spaceKey, String category) {
        return Mono.fromCallable(() -> {
                    AtomicBoolean lock = spaceLocks.computeIfAbsent(spaceKey, k -> new AtomicBoolean(false));
                    if (!lock.compareAndSet(false, true)) {
                        log.warn("Попытка запустить краулинг для пространства '{}', но он уже выполняется.", spaceKey);
                        return false;
                    }
                    return true;
                })
                .flatMap(started -> {
                    if (!started) {
                        return Mono.just(false);
                    }

                    log.info("Начало асинхронного краулинга пространства Confluence: {}, категория: {}", spaceKey, category);
                    // Запускаем всю цепочку асинхронно
                    executeCrawling(spaceKey, category)
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    null, // onNext не нужен
                                    error -> log.error("Ошибка во время краулинга пространства {}:", spaceKey, error)
                            );

                    return Mono.just(true);
                });
    }

    /**
     * Инкапсулирует полную реактивную цепочку краулинга и индексации.
     *
     * @param spaceKey Ключ пространства.
     * @param category Категория.
     * @return {@link Mono<Void>}, завершающийся после обработки всех страниц.
     */
    private Mono<Void> executeCrawling(String spaceKey, String category) {
        return confluenceApiClient.fetchAllPagesInSpace(spaceKey)
                .flatMap(page -> confluenceApiClient.getPageContent(page.id())
                        .flatMap(content -> ingestPageContent(content, category))
                )
                .doOnComplete(() -> log.info("Краулинг пространства {} успешно завершен.", spaceKey))
                .doFinally(signalType -> {
                    // Гарантированно снимаем блокировку при завершении или ошибке
                    spaceLocks.get(spaceKey).set(false);
                    log.info("Блокировка для пространства '{}' снята.", spaceKey);
                })
                .then();
    }


    /**
     * Обрабатывает контент одной страницы и отправляет его на индексацию.
     *
     * @param page     DTO с контентом страницы.
     * @param category Категория для метаданных.
     * @return {@link Mono<Void>}, завершающийся после отправки на индексацию.
     */
    private Mono<Void> ingestPageContent(ConfluencePageDto page, String category) {
        if (page == null || page.body() == null || page.body().storage() == null || page.body().storage().value().isBlank()) {
            log.warn("Пропуск страницы '{}' (ID: {}) из-за отсутствия контента.", page.title(), page.id());
            return Mono.empty();
        }

        String cleanText = Jsoup.parse(page.body().storage().value()).text();
        String sourceName = String.format("Confluence-%s: %s", page.spaceKey(), page.title());

        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("doc_type", "confluence_page");
        metadata.put("doc_category", category);
        metadata.put("confluence_id", page.id());
        metadata.put("confluence_url", page.links().webui());
        if (page.version() != null && page.version().when() != null) {
            metadata.put("last_modified", page.version().when());
        }

        IndexingRequest request = new IndexingRequest(
                page.id(),
                sourceName,
                cleanText,
                metadata
        );

        log.debug("Страница '{}' (ID: {}) с категорией '{}' отправлена на индексацию.", page.title(), page.id(), category);
        return indexingPipelineService.process(request);
    }
}
