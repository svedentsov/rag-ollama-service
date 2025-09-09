package com.example.ragollama.rag.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryProcessingPipeline {

    private final HyDEAgent hydeAgent;
    private final QueryTransformationAgent transformationAgent;
    private final MultiQueryGeneratorAgent multiQueryGeneratorAgent;
    private final StepBackQueryAgent stepBackQueryAgent;

    public Mono<ProcessedQueries> process(String query) {
        log.info("Запуск последовательного конвейера обработки запроса.");
        // Этап 1: HyDE - самый мощный, запускаем его первым.
        return hydeAgent.enhance(query)
                .flatMap(hydeResult -> {
                    String primaryQuery = hydeResult.isEmpty() ? query : hydeResult.get(0);
                    List<String> expansionQueries = new ArrayList<>();
                    // Этап 2: Трансформация - для извлечения ключевых слов.
                    return transformationAgent.enhance(query)
                            .flatMap(transformedResult -> {
                                expansionQueries.addAll(transformedResult);
                                // Этап 3 (опциональный): Если трансформация не дала хороших ключевых слов,
                                // запускаем MultiQuery и StepBack для генерации идей.
                                if (transformedResult.size() <= 1 && transformedResult.get(0).equals(query)) {
                                    log.debug("Трансформация не дала результата, запускаем MultiQuery и StepBack.");
                                    Mono<List<String>> multiQueryMono = multiQueryGeneratorAgent.enhance(query);
                                    Mono<List<String>> stepBackMono = stepBackQueryAgent.enhance(query);
                                    return Mono.zip(multiQueryMono, stepBackMono)
                                            .map(tuple -> {
                                                expansionQueries.addAll(tuple.getT1());
                                                expansionQueries.addAll(tuple.getT2());
                                                return buildResult(query, primaryQuery, expansionQueries);
                                            });
                                }
                                return Mono.just(buildResult(query, primaryQuery, expansionQueries));
                            });
                });
    }

    private ProcessedQueries buildResult(String originalQuery, String primaryQuery, List<String> expansionQueries) {
        List<String> uniqueExpansion = expansionQueries.stream()
                .filter(q -> !q.equalsIgnoreCase(primaryQuery) && !q.equalsIgnoreCase(originalQuery))
                .distinct()
                .toList();

        ProcessedQueries result = new ProcessedQueries(primaryQuery, uniqueExpansion);
        log.info("Конвейер завершен. Primary: '{}...', Expansion ({} шт): {}",
                StringUtils.left(result.primaryQuery(), 70), result.expansionQueries().size(), result.expansionQueries());
        return result;
    }
}
