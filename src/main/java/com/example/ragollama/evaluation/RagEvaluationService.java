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
import org.springframework.ai.document.Document;
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

/**
 * Сервис для выполнения оффлайн-оценки качества RAG-системы по "золотому датасету".
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
     */
    public Mono<EvaluationResult> evaluate() {
        try {
            List<GoldenRecord> dataset = loadGoldenDataset();
            log.info("Начинается оценка по {} записям из 'золотого датасета'.", dataset.size());

            Map<String, EvaluationResult.RecordResult> details = new ConcurrentHashMap<>();
            Set<String> failures = ConcurrentHashMap.newKeySet();

            return Flux.fromIterable(dataset)
                    .flatMap(record -> evaluateRecord(record)
                                    .doOnNext(resultTuple -> details.put(resultTuple.getT1(), resultTuple.getT2()))
                                    .doOnError(e -> {
                                        log.error("Ошибка во время оценки записи ID: {}", record.queryId(), e);
                                        failures.add(record.queryId());
                                    })
                                    .onErrorResume(e -> Mono.empty()),
                            4)
                    .then(Mono.fromCallable(() -> calculateFinalResults(dataset.size(), details, failures)));

        } catch (IOException e) {
            log.error("Не удалось загрузить 'золотой датасет' из {}", GOLDEN_DATASET_PATH, e);
            return Mono.error(new IllegalStateException("Ошибка загрузки датасета", e));
        }
    }

    /**
     * Оценивает одну запись из "золотого датасета".
     */
    private Mono<Tuple2<String, EvaluationResult.RecordResult>> evaluateRecord(GoldenRecord record) {
        var retrievalConfig = retrievalProperties.hybrid().vectorSearch();
        return queryProcessingPipeline.process(record.queryText())
                .flatMap(processedQueries -> retrievalStrategy.retrieve(
                        processedQueries,
                        record.queryText(),
                        retrievalConfig.topK(),
                        retrievalConfig.similarityThreshold(),
                        null // Фильтр не используется при общей оценке
                ))
                .map(retrievedDocs -> {
                    Set<String> retrievedIds = new HashSet<>();
                    for (Document doc : retrievedDocs) {
                        Object docId = doc.getMetadata().get("documentId");
                        if (docId != null) {
                            retrievedIds.add(docId.toString());
                        }
                    }

                    Set<String> expectedIds = record.expectedDocumentIds();
                    Set<String> intersection = new HashSet<>(retrievedIds);
                    intersection.retainAll(expectedIds);

                    int tp = intersection.size();
                    int fp = retrievedIds.size() - tp;
                    int fn = expectedIds.size() - tp;

                    double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
                    double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;

                    var result = new EvaluationResult.RecordResult(precision, recall, expectedIds.size(), retrievedIds.size(), tp);
                    return Tuples.of(record.queryId(), result);
                });
    }

    /**
     * Вычисляет итоговые агрегированные метрики.
     */
    private EvaluationResult calculateFinalResults(int total, Map<String, EvaluationResult.RecordResult> details, Set<String> failures) {
        double avgPrecision = details.values().stream().mapToDouble(EvaluationResult.RecordResult::precision).average().orElse(0.0);
        double avgRecall = details.values().stream().mapToDouble(EvaluationResult.RecordResult::recall).average().orElse(0.0);
        double f1Score = (avgPrecision + avgRecall) > 0 ? 2 * (avgPrecision * avgRecall) / (avgPrecision + avgRecall) : 0.0;

        log.info("Оценка завершена. Recall: {:.2f}, Precision: {:.2f}, F1-Score: {:.2f}", avgRecall, avgPrecision, f1Score);
        return new EvaluationResult(total, avgPrecision, avgRecall, f1Score, new ArrayList<>(failures), details);
    }

    /**
     * Загружает и десериализует "золотой датасет".
     */
    private List<GoldenRecord> loadGoldenDataset() throws IOException {
        Resource resource = resourceLoader.getResource(GOLDEN_DATASET_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        }
    }
}
