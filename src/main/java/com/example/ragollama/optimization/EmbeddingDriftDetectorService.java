package com.example.ragollama.optimization;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingDriftDetectorService {

    private final EmbeddingModel embeddingModel;
    private final MeterRegistry meterRegistry;

    // Порог, при превышении которого будет зафиксирован дрейф
    private static final double DRIFT_THRESHOLD = 0.05;

    // Эталонные данные. В реальной системе они бы загружались из файла/БД.
    private static final List<String> REFERENCE_DOCUMENTS = List.of(
            "Что такое Spring AI?",
            "Как работает pgvector?",
            "Принципы SOLID в разработке"
    );

    private final AtomicReference<List<float[]>> referenceEmbeddings = new AtomicReference<>();

    public void detectDrift() {
        if (referenceEmbeddings.get() == null) {
            log.info("Создание эталонных эмбеддингов для мониторинга дрейфа...");
            referenceEmbeddings.set(embeddingModel.embed(REFERENCE_DOCUMENTS));
            log.info("Эталонные эмбеддинги успешно созданы и сохранены в памяти.");
            return;
        }

        log.info("Запуск проверки дрейфа эмбеддингов...");
        List<float[]> currentEmbeddings = embeddingModel.embed(REFERENCE_DOCUMENTS);
        List<float[]> reference = referenceEmbeddings.get();

        double totalDistance = 0.0;
        for (int i = 0; i < reference.size(); i++) {
            totalDistance += calculateCosineDistance(reference.get(i), currentEmbeddings.get(i));
        }
        double avgDistance = totalDistance / reference.size();

        meterRegistry.gauge("rag.embeddings.drift.distance", avgDistance);

        if (avgDistance > DRIFT_THRESHOLD) {
            log.error("""
                    
                    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    !!! ВНИМАНИЕ: ОБНАРУЖЕН КРИТИЧЕСКИЙ ДРЕЙФ ЭМБЕДДИНГОВ !!!
                    !!! Среднее косинусное расстояние: {} (Порог: {})    !!!
                    !!! Требуется полная переиндексация базы знаний!     !!!
                    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    """, String.format("%.4f", avgDistance), DRIFT_THRESHOLD);
        } else {
            log.info("Проверка дрейфа эмбеддингов завершена. Дрейф не обнаружен. Среднее расстояние: {}", String.format("%.4f", avgDistance));
        }
    }

    /**
     * Метод теперь принимает массивы примитивов float[]
     */
    private double calculateCosineDistance(float[] v1, float[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("Векторы должны иметь одинаковую размерность");
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            normA += Math.pow(v1[i], 2);
            normB += Math.pow(v2[i], 2);
        }
        // Защита от деления на ноль, если один из векторов нулевой
        if (normA == 0 || normB == 0) {
            return 1.0; // Максимальное расстояние
        }
        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        return 1 - similarity;
    }
}
