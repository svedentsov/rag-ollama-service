package com.example.ragollama.rag.agent;

import com.example.ragollama.rag.domain.model.QueryFormationStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис, реализующий последовательный конвейер для обработки и улучшения
 * пользовательского запроса перед этапом извлечения.
 * <p>
 * Эта версия не просто возвращает результат, а строит детальную историю
 * всех шагов трансформации, обеспечивая полную прозрачность процесса.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryProcessingPipeline {

    private final HyDEAgent hydeAgent;
    private final QueryTransformationAgent transformationAgent;
    private final MultiQueryGeneratorAgent multiQueryGeneratorAgent;
    private final StepBackQueryAgent stepBackQueryAgent;

    /**
     * Выполняет полную цепочку обработки запроса и возвращает результат вместе с историей.
     *
     * @param query Исходный запрос пользователя.
     * @return {@link Mono} с объектом {@link ProcessedQueries}, содержащим финальные
     *         запросы для поиска и историю их формирования.
     */
    public Mono<ProcessedQueries> process(String query) {
        log.info("Запуск конвейера обработки запроса для: '{}'", query);
        List<QueryFormationStep> history = new ArrayList<>();
        List<String> expansionQueries = new ArrayList<>();

        // Этап 1: HyDE - самый мощный, запускаем его первым для определения основного вектора поиска.
        return hydeAgent.enhance(query)
                .flatMap(hydeResult -> {
                    String primaryQuery = hydeResult.isEmpty() ? query : hydeResult.get(0);
                    history.add(new QueryFormationStep(
                            "HyDEAgent",
                            "Сгенерирован гипотетический документ для семантического поиска.",
                            primaryQuery
                    ));

                    // Этап 2: Трансформация - для извлечения ключевых слов.
                    return transformationAgent.enhance(query)
                            .flatMap(transformedResult -> {
                                expansionQueries.addAll(transformedResult);
                                history.add(new QueryFormationStep(
                                        "QueryTransformationAgent",
                                        "Извлечены ключевые термины для гибридного поиска.",
                                        String.join(", ", transformedResult)
                                ));

                                // Этап 3 (опциональный): Если трансформация не дала хороших результатов,
                                // запускаем MultiQuery и StepBack для генерации альтернативных формулировок.
                                if (isTransformationIneffective(query, transformedResult)) {
                                    log.debug("Трансформация не дала результата, запускаем MultiQuery и StepBack.");
                                    Mono<List<String>> multiQueryMono = multiQueryGeneratorAgent.enhance(query);
                                    Mono<List<String>> stepBackMono = stepBackQueryAgent.enhance(query);

                                    return Mono.zip(multiQueryMono, stepBackMono)
                                            .map(tuple -> {
                                                expansionQueries.addAll(tuple.getT1());
                                                history.add(new QueryFormationStep("MultiQueryAgent", "Сгенерированы альтернативные формулировки.", tuple.getT1()));
                                                expansionQueries.addAll(tuple.getT2());
                                                history.add(new QueryFormationStep("StepBackQueryAgent", "Сгенерирован обобщенный концептуальный запрос.", tuple.getT2().get(0)));
                                                return buildResult(query, primaryQuery, expansionQueries, history);
                                            });
                                }
                                return Mono.just(buildResult(query, primaryQuery, expansionQueries, history));
                            });
                });
    }

    private boolean isTransformationIneffective(String originalQuery, List<String> transformedResult) {
        return transformedResult.size() <= 1 && transformedResult.getFirst().equals(originalQuery);
    }

    private ProcessedQueries buildResult(String originalQuery, String primaryQuery, List<String> expansionQueries, List<QueryFormationStep> history) {
        List<String> uniqueExpansion = expansionQueries.stream()
                .filter(q -> !q.equalsIgnoreCase(primaryQuery) && !q.equalsIgnoreCase(originalQuery))
                .distinct()
                .toList();

        ProcessedQueries result = new ProcessedQueries(primaryQuery, uniqueExpansion, history);
        log.info("Конвейер обработки запроса завершен. Primary: '{}...', Expansion ({} шт): {}",
                StringUtils.left(result.primaryQuery(), 70), result.expansionQueries().size(), result.expansionQueries());
        return result;
    }
}
