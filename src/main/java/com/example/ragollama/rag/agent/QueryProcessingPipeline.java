package com.example.ragollama.rag.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Сервис-оркестратор, управляющий конвейером AI-агентов для обработки запросов.
 * <p>
 * Эта версия возвращает структурированный объект {@link ProcessedQueries},
 * явно разделяя "основной" (трансформированный) запрос от "расширенных"
 * (сгенерированных), что позволяет вызывающей стороне строить более
 * интеллектуальные стратегии извлечения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryProcessingPipeline {

    private final List<QueryEnhancementAgent> agents;

    /**
     * Асинхронно выполняет весь конвейер агентов для заданного запроса.
     *
     * @param query Оригинальный запрос пользователя.
     * @return {@link Mono}, который по завершении будет содержать объект
     * {@link ProcessedQueries} с разделенными запросами.
     */
    public Mono<ProcessedQueries> process(String query) {
        log.info("Запуск конвейера обработки запроса с {} агентами.", agents.size());
        if (agents.isEmpty()) {
            return Mono.just(new ProcessedQueries(query, List.of()));
        }

        AtomicReference<String> primaryQueryRef = new AtomicReference<>(query);
        List<String> expansionQueries = new ArrayList<>();

        return Flux.fromIterable(agents)
                .flatMap(agent -> agent.enhance(query)
                        .doOnNext(results -> {
                            if (agent instanceof QueryTransformationAgent && !results.isEmpty()) {
                                primaryQueryRef.set(results.getFirst());
                            } else {
                                expansionQueries.addAll(results);
                            }
                        }))
                .then(Mono.fromCallable(() -> {
                    // Удаляем дубликаты и возможный primary query из списка расширенных
                    expansionQueries.remove(primaryQueryRef.get());
                    List<String> uniqueExpansion = expansionQueries.stream().distinct().toList();
                    ProcessedQueries result = new ProcessedQueries(primaryQueryRef.get(), uniqueExpansion);
                    log.info("Конвейер завершен. Primary: '{}', Expansion ({} шт): {}",
                            result.primaryQuery(), result.expansionQueries().size(), result.expansionQueries());
                    return result;
                }));
    }
}
