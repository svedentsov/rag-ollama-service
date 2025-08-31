package com.example.ragollama.evaluation;

import com.example.ragollama.evaluation.model.EvaluationResult;
import com.example.ragollama.evaluation.model.GoldenRecord;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Сервис для выполнения оффлайн-оценки качества RAG-системы по "золотому датасету".
 * <p>
 * Этот сервис является ядром MLOps-контура. Он позволяет автоматически и
 * объективно измерять качество поиска (retrieval), которое является
 * фундаментом для качества всей RAG-системы.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagEvaluationService {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final QueryProcessingPipeline queryProcessingPipeline;
    private final HybridRetrievalStrategy retrievalStrategy;
    private final RetrievalProperties retrievalProperties;

    private static final String GOLDEN_DATASET_PATH = "classpath:evaluation/golden-dataset.json";

    /**
     * Запускает полный цикл оценки качества RAG-системы.
     * <p>
     * Процесс полностью асинхронен и распараллелен для максимальной производительности.
     * Он читает "золотой датасет", для каждой записи выполняет RAG-поиск,
     * сравнивает результаты с эталоном и агрегирует итоговые метрики.
     *
     * @return {@link Mono}, который по завершении всех вычислений будет содержать
     * объект {@link EvaluationResult} с итоговыми метриками.
     */
    public Mono<EvaluationResult> evaluate() {
        try {
            List<GoldenRecord> dataset = loadGoldenDataset();
            if (dataset.isEmpty()) {
                log.warn("'Золотой датасет' пуст. Оценка не будет проводиться.");
                return Mono.just(new EvaluationResult(0, 0, 0, 0, List.of(), Map.of()));
            }
            log.info("Начинается оценка по {} записям из 'золотого датасета'.", dataset.size());

            Map<String, EvaluationResult.RecordResult> details = new ConcurrentHashMap<>();
            Set<String> failures = ConcurrentHashMap.newKeySet();
            final int concurrency = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

            return Flux.fromIterable(dataset)
                    .flatMap(record -> evaluateRecord(record)
                                    .doOnNext(resultTuple -> details.put(resultTuple.getT1(), resultTuple.getT2()))
                                    .doOnError(e -> {
                                        log.error("Ошибка во время оценки записи ID: {}", record.queryId(), e);
                                        failures.add(record.queryId());
                                    })
                                    .onErrorResume(e -> Mono.empty()),
                            concurrency) // Выполняем до N запросов параллельно
                    .then(Mono.fromCallable(() -> calculateFinalResults(dataset.size(), details, failures)));

        } catch (IOException e) {
            log.error("Не удалось загрузить 'золотой датасет' из {}", GOLDEN_DATASET_PATH, e);
            return Mono.error(new IllegalStateException("Ошибка загрузки датасета", e));
        }
    }

    /**
     * Оценивает одну запись из "золотого датасета", выполняя RAG-поиск и
     * вычисляя метрики Precision и Recall.
     *
     * @param record Одна запись "вопрос-ответ" из эталонного набора.
     * @return {@link Mono} с кортежем, содержащим ID записи и ее результат.
     */
    private Mono<Tuple2<String, EvaluationResult.RecordResult>> evaluateRecord(GoldenRecord record) {
        var retrievalConfig = retrievalProperties.hybrid().vectorSearch();
        return queryProcessingPipeline.process(record.queryText())
                .flatMap(processedQueries -> retrievalStrategy.retrieve(
                        processedQueries,
                        record.queryText(),
                        retrievalConfig.topK(),
                        retrievalConfig.similarityThreshold(),
                        null
                ))
                .map(retrievedDocs -> {
                    Set<String> retrievedIds = retrievedDocs.stream()
                            .map(doc -> Objects.toString(doc.getMetadata().get("documentId"), null))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    Set<String> expectedIds = record.expectedDocumentIds();
                    Set<String> intersection = new HashSet<>(retrievedIds);
                    intersection.retainAll(expectedIds);

                    int tp = intersection.size(); // True Positives
                    int fp = retrievedIds.size() - tp; // False Positives
                    int fn = expectedIds.size() - tp; // False Negatives

                    double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
                    double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;

                    var result = new EvaluationResult.RecordResult(precision, recall, expectedIds.size(), retrievedIds.size(), tp);
                    return Tuples.of(record.queryId(), result);
                });
    }

    /**
     * Вычисляет итоговые агрегированные метрики по результатам всех записей.
     *
     * @param total    Общее количество записей в датасете.
     * @param details  Карта с результатами по каждой успешно обработанной записи.
     * @param failures Множество ID записей, обработка которых завершилась ошибкой.
     * @return Финальный объект {@link EvaluationResult}.
     */
    private EvaluationResult calculateFinalResults(int total, Map<String, EvaluationResult.RecordResult> details, Set<String> failures) {
        double avgPrecision = details.values().stream().mapToDouble(EvaluationResult.RecordResult::precision).average().orElse(0.0);
        double avgRecall = details.values().stream().mapToDouble(EvaluationResult.RecordResult::recall).average().orElse(0.0);
        double f1Score = (avgPrecision + avgRecall) > 0 ? 2 * (avgPrecision * avgRecall) / (avgPrecision + avgRecall) : 0.0;

        log.info("Оценка завершена. Recall: {:.4f}, Precision: {:.4f}, F1-Score: {:.4f}, Failures: {}", avgRecall, avgPrecision, f1Score, failures.size());
        return new EvaluationResult(total, avgPrecision, avgRecall, f1Score, new ArrayList<>(failures), details);
    }

    /**
     * Загружает и десериализует "золотой датасет" из JSON-файла в ресурсах.
     *
     * @return Список объектов {@link GoldenRecord}.
     * @throws IOException если файл не найден или имеет некорректный формат.
     */
    private List<GoldenRecord> loadGoldenDataset() throws IOException {
        Resource resource = resourceLoader.getResource(GOLDEN_DATASET_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        }
    }
}
