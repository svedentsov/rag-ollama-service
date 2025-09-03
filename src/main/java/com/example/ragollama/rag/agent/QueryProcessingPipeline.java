package com.example.ragollama.rag.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Сервис-оркестратор, управляющий конвейером AI-агентов для обработки запросов.
 * Эта версия возвращает структурированный объект {@link ProcessedQueries},
 * явно разделяя "основной" (трансформированный) запрос от "расширенных"
 * (сгенерированных), что позволяет вызывающей стороне строить более
 * интеллектуальные стратегии извлечения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryProcessingPipeline {

    private final QueryTransformationAgent transformationAgent;
    private final MultiQueryGeneratorAgent multiQueryGeneratorAgent;

    /**
     * Асинхронно выполняет весь конвейер агентов для заданного запроса.
     *
     * @param query Оригинальный запрос пользователя.
     * @return {@link Mono}, который по завершении будет содержать объект
     * {@link ProcessedQueries} с разделенными запросами.
     */
    public Mono<ProcessedQueries> process(String query) {
        log.info("Запуск параллельного конвейера обработки запроса.");

        Mono<List<String>> transformationMono = transformationAgent.enhance(query);
        Mono<List<String>> expansionMono = multiQueryGeneratorAgent.enhance(query);

        return Mono.zip(transformationMono, expansionMono)
                .map(tuple -> {
                    String primaryQuery = tuple.getT1().isEmpty() ? query : tuple.getT1().getFirst();
                    List<String> expansionQueries = new ArrayList<>(tuple.getT2());
                    // Удаляем дубликаты и основной запрос из списка расширенных
                    expansionQueries.remove(query);
                    expansionQueries.remove(primaryQuery);
                    List<String> uniqueExpansion = expansionQueries.stream().distinct().toList();
                    ProcessedQueries result = new ProcessedQueries(primaryQuery, uniqueExpansion);
                    log.info("Конвейер завершен. Primary: '{}', Expansion ({} шт): {}",
                            result.primaryQuery(), result.expansionQueries().size(), result.expansionQueries());
                    return result;
                });
    }
}
