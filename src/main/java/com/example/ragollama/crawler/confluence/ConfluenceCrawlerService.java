package com.example.ragollama.crawler.confluence;

import com.example.ragollama.ingestion.api.dto.DocumentIngestionRequest;
import com.example.ragollama.ingestion.domain.DocumentIngestionService;
import com.example.ragollama.qaagent.tools.ConfluenceApiClient;
import com.example.ragollama.qaagent.tools.ConfluencePageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сервис-оркестратор для выполнения краулинга пространств Confluence.
 * <p>
 * Отвечает за обход дерева страниц, извлечение контента и его передачу
 * в сервис индексации. Реализует простую блокировку для предотвращения
 * одновременного запуска нескольких задач для одного и того же пространства.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceCrawlerService {

    private final ConfluenceApiClient confluenceApiClient;
    private final DocumentIngestionService documentIngestionService;
    private final Map<String, AtomicBoolean> spaceLocks = new ConcurrentHashMap<>();

    /**
     * Асинхронно запускает полный краулинг указанного пространства Confluence.
     * <p>
     * Метод выполняется в отдельном потоке, чтобы не блокировать основной
     * поток приложения.
     *
     * @param spaceKey Ключ пространства для краулинга.
     * @return {@code true}, если задача была успешно запущена, {@code false} - если
     * задача для этого пространства уже выполняется.
     */
    @Async("applicationTaskExecutor")
    public boolean crawlSpaceAsync(String spaceKey) {
        AtomicBoolean lock = spaceLocks.computeIfAbsent(spaceKey, k -> new AtomicBoolean(false));
        if (!lock.compareAndSet(false, true)) {
            log.warn("Попытка запустить краулинг для пространства '{}', но он уже выполняется.", spaceKey);
            return false;
        }

        log.info("Начало асинхронного краулинга пространства Confluence: {}", spaceKey);
        try {
            confluenceApiClient.fetchAllPagesInSpace(spaceKey)
                    .flatMap(page -> confluenceApiClient.getPageContent(page.id())
                            .doOnNext(this::ingestPageContent)
                    )
                    .doOnComplete(() -> log.info("Краулинг пространства {} успешно завершен.", spaceKey))
                    .doOnError(error -> log.error("Ошибка во время краулинга пространства {}:", spaceKey, error))
                    .blockLast(); // Блокируемся до завершения потока в рамках @Async метода
        } finally {
            lock.set(false);
            log.info("Блокировка для пространства '{}' снята.", spaceKey);
        }
        return true;
    }

    /**
     * Обрабатывает контент одной страницы и отправляет его на индексацию.
     *
     * @param page DTO с контентом страницы.
     */
    private void ingestPageContent(ConfluencePageDto page) {
        if (page == null || page.body() == null || page.body().storage() == null || page.body().storage().value().isBlank()) {
            log.warn("Пропуск страницы '{}' (ID: {}) из-за отсутствия контента.", page.title(), page.id());
            return;
        }

        // Confluence хранит контент в XHTML, очищаем его от тегов с помощью Jsoup
        String cleanText = Jsoup.parse(page.body().storage().value()).text();
        String sourceName = String.format("Confluence-%s: %s", page.spaceKey(), page.title());

        DocumentIngestionRequest request = new DocumentIngestionRequest(
                sourceName,
                cleanText,
                Map.of(
                        "doc_type", "confluence_page",
                        "confluence_id", page.id(),
                        "confluence_url", page.links().webui()
                )
        );

        documentIngestionService.scheduleDocumentIngestion(request);
        log.debug("Страница '{}' (ID: {}) поставлена в очередь на индексацию.", page.title(), page.id());
    }
}
