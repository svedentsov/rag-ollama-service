package com.example.ragollama.rag.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис-оркестратор, управляющий конвейером AI-агентов для обработки запросов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryProcessingPipeline {

    private final QueryTransformationAgent transformationAgent;
    private final MultiQueryGeneratorAgent multiQueryGeneratorAgent;
    private final StepBackQueryAgent stepBackQueryAgent; // НОВАЯ ЗАВИСИМОСТЬ

    /**
     * Асинхронно выполняет весь конвейер агентов для заданного запроса.
     */
    public Mono<ProcessedQueries> process(String query) {
        log.info("Запуск параллельного конвейера обработки запроса.");

        Mono<List<String>> transformationMono = transformationAgent.enhance(query);
        Mono<List<String>> expansionMono = multiQueryGeneratorAgent.enhance(query);
        Mono<List<String>> stepBackMono = stepBackQueryAgent.enhance(query);

        return Mono.zip(transformationMono, expansionMono, stepBackMono)
                .map(tuple -> {
                    String primaryQuery = tuple.getT1().isEmpty() ? query : tuple.getT1().getFirst();
                    List<String> expansionQueries = new ArrayList<>();
                    expansionQueries.addAll(tuple.getT2());
                    expansionQueries.addAll(tuple.getT3());
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
