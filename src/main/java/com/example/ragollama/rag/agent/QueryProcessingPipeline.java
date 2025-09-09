package com.example.ragollama.rag.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    private final HyDEAgent hydeAgent;
    private final QueryTransformationAgent transformationAgent;
    private final MultiQueryGeneratorAgent multiQueryGeneratorAgent;
    private final StepBackQueryAgent stepBackQueryAgent;

    /**
     * Асинхронно выполняет весь конвейер агентов для заданного запроса.
     */
    public Mono<ProcessedQueries> process(String query) {
        log.info("Запуск параллельного конвейера обработки запроса с HyDE.");
        Mono<List<String>> hydeMono = hydeAgent.enhance(query);
        Mono<List<String>> transformationMono = transformationAgent.enhance(query);
        Mono<List<String>> expansionMono = multiQueryGeneratorAgent.enhance(query);
        Mono<List<String>> stepBackMono = stepBackQueryAgent.enhance(query);
        return Mono.zip(hydeMono, transformationMono, expansionMono, stepBackMono)
                .map(tuple -> {
                    // Используем результат HyDE как основной (самый сильный) поисковый запрос
                    String primaryQuery = tuple.getT1().isEmpty() ? query : tuple.getT1().getFirst();
                    List<String> expansionQueries = new ArrayList<>();
                    expansionQueries.addAll(tuple.getT2()); // Трансформированный запрос
                    expansionQueries.addAll(tuple.getT3()); // Multi-query
                    expansionQueries.addAll(tuple.getT4()); // Step-back
                    // Удаляем дубликаты и основной запрос из списка расширений
                    List<String> uniqueExpansion = expansionQueries.stream()
                            .filter(q -> !q.equalsIgnoreCase(primaryQuery) && !q.equalsIgnoreCase(query))
                            .distinct()
                            .toList();
                    ProcessedQueries result = new ProcessedQueries(primaryQuery, uniqueExpansion);
                    log.info("Конвейер завершен. Primary (HyDE): '{}...', Expansion ({} шт): {}",
                            StringUtils.left(result.primaryQuery(), 70), result.expansionQueries().size(), result.expansionQueries());
                    return result;
                });
    }
}
