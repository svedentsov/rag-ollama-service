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

/**
 * Сервис для выполнения оффлайн-оценки качества RAG-системы по "золотому датасету".
 * В этой версии добавлено ограничение параллелизма для снижения нагрузки.
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

    public Mono<EvaluationResult> evaluate() {
        try {
            List<GoldenRecord> dataset = loadGoldenDataset();
            if (dataset.isEmpty()) {
                log.warn("'Золотой датасет' пуст. Оценка не будет проводиться.");
                return Mono.just(new EvaluationResult(0, 0, 0, 0, 0, 0, List.of(), Map.of()));
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
                            concurrency) // Ограничиваем параллелизм
                    .then(Mono.fromCallable(() -> calculateFinalResults(dataset.size(), details, failures)));

        } catch (IOException e) {
            log.error("Не удалось загрузить 'золотой датасет' из {}", GOLDEN_DATASET_PATH, e);
            return Mono.error(new IllegalStateException("Ошибка загрузки датасета", e));
        }
    }

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
                    List<String> retrievedIds = retrievedDocs.stream()
                            .map(doc -> Objects.toString(doc.getMetadata().get("documentId"), null))
                            .filter(Objects::nonNull)
                            .toList();

                    Set<String> expectedIds = record.expectedDocumentIds();
                    Set<String> intersection = new HashSet<>(retrievedIds);
                    intersection.retainAll(expectedIds);

                    int tp = intersection.size();
                    int fp = retrievedIds.size() - tp;
                    int fn = expectedIds.size() - tp;

                    double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
                    double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;

                    double reciprocalRank = calculateReciprocalRank(retrievedIds, expectedIds);
                    double dcg = calculateDcg(retrievedIds, expectedIds, 5);
                    double idcg = calculateIdealDcg(expectedIds.size(), 5);

                    var result = new EvaluationResult.RecordResult(
                            precision, recall, reciprocalRank, dcg, idcg,
                            expectedIds.size(), retrievedIds.size(), tp);
                    return Tuples.of(record.queryId(), result);
                });
    }

    private EvaluationResult calculateFinalResults(int total, Map<String, EvaluationResult.RecordResult> details, Set<String> failures) {
        double avgPrecision = details.values().stream().mapToDouble(EvaluationResult.RecordResult::precision).average().orElse(0.0);
        double avgRecall = details.values().stream().mapToDouble(EvaluationResult.RecordResult::recall).average().orElse(0.0);
        double f1Score = (avgPrecision + avgRecall) > 0 ? 2 * (avgPrecision * avgRecall) / (avgPrecision + avgRecall) : 0.0;

        double mrr = details.values().stream().mapToDouble(EvaluationResult.RecordResult::reciprocalRank).average().orElse(0.0);
        double ndcg = details.values().stream()
                .mapToDouble(r -> r.idcgAt5() > 0 ? r.dcgAt5() / r.idcgAt5() : 0.0)
                .average().orElse(0.0);

        log.info("Оценка завершена. Recall: {:.4f}, Precision: {:.4f}, F1-Score: {:.4f}, MRR: {:.4f}, NDCG@5: {:.4f}, Failures: {}",
                avgRecall, avgPrecision, f1Score, mrr, ndcg, failures.size());
        return new EvaluationResult(total, avgPrecision, avgRecall, f1Score, mrr, ndcg, new ArrayList<>(failures), details);
    }

    private List<GoldenRecord> loadGoldenDataset() throws IOException {
        Resource resource = resourceLoader.getResource(GOLDEN_DATASET_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        }
    }

    private double calculateReciprocalRank(List<String> retrieved, Set<String> expected) {
        for (int i = 0; i < retrieved.size(); i++) {
            if (expected.contains(retrieved.get(i))) {
                return 1.0 / (i + 1.0);
            }
        }
        return 0.0;
    }

    private double calculateDcg(List<String> retrieved, Set<String> expected, int k) {
        double dcg = 0.0;
        for (int i = 0; i < Math.min(retrieved.size(), k); i++) {
            if (expected.contains(retrieved.get(i))) {
                dcg += 1.0 / (Math.log(i + 2) / Math.log(2)); // log base 2
            }
        }
        return dcg;
    }

    private double calculateIdealDcg(int totalRelevant, int k) {
        double idcg = 0.0;
        for (int i = 0; i < Math.min(totalRelevant, k); i++) {
            idcg += 1.0 / (Math.log(i + 2) / Math.log(2));
        }
        return idcg;
    }
}
