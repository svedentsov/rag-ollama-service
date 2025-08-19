package com.example.ragollama.rag.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Сервис-оркестратор, управляющий конвейером AI-агентов для обработки запросов.
 * Spring автоматически внедряет в конструктор список всех бинов, реализующих
 * интерфейс {@link QueryEnhancementAgent}, отсортированный согласно их аннотации {@code @Order}.
 * Сервис запускает всех агентов параллельно, собирает их результаты и
 * формирует финальный, уникализированный список запросов для этапа Retrieval.
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
     * @return {@link Mono}, который по завершении будет содержать единый список
     * уникальных, обработанных запросов.
     */
    public Mono<List<String>> process(String query) {
        log.info("Запуск конвейера обработки запроса с {} агентами.", agents.size());
        if (agents.isEmpty()) {
            return Mono.just(List.of(query));
        }

        return Flux.fromIterable(agents)
                .flatMap(agent -> agent.enhance(query)
                        .doOnNext(results -> log.debug("Агент '{}' вернул: {}",
                                agent.getClass().getSimpleName(), results)))
                .flatMap(Flux::fromIterable) // "Расплющиваем" списки в один поток строк
                .distinct() // Оставляем только уникальные запросы
                .collectList()
                .doOnSuccess(finalQueries -> log.info("Конвейер завершен. Итоговый список запросов ({} шт): {}",
                        finalQueries.size(), finalQueries));
    }
}
