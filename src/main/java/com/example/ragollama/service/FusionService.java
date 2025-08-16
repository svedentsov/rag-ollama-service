package com.example.ragollama.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис, реализующий слияние результатов из различных источников поиска
 * с использованием алгоритма Reciprocal Rank Fusion (RRF).
 * RRF позволяет эффективно объединять ранжированные списки, не требуя
 * нормализации оценок релевантности, что делает его идеальным для гибридного поиска.
 */
@Service
@Slf4j
public class FusionService {

    /**
     * Константа для сглаживания в формуле RRF. Предотвращает чрезмерное
     * доминирование документов, находящихся на самой первой позиции.
     * Значение 60 является общепринятым стандартом.
     */
    private static final int RRF_K = 60;

    /**
     * Объединяет несколько списков документов с помощью Reciprocal Rank Fusion.
     *
     * @param documentLists Список, содержащий результаты от каждого источника поиска.
     * @return Единый, переранжированный и отсортированный список уникальных документов.
     */
    public List<Document> reciprocalRankFusion(List<List<Document>> documentLists) {
        if (documentLists == null || documentLists.isEmpty()) {
            return List.of();
        }

        // Шаг 1: Группируем все документы по их ID и вычисляем RRF-оценку для каждого.
        Map<String, RrfScore> scores = documentLists.stream()
                .flatMap(list -> {
                    // Преобразуем список документов в поток с рангами (начиная с 1)
                    return java.util.stream.IntStream.range(0, list.size())
                            .mapToObj(i -> new RankedDocument(list.get(i), i + 1));
                })
                .collect(Collectors.toMap(
                        rankedDoc -> rankedDoc.document().getId(), // Ключ - ID документа
                        rankedDoc -> new RrfScore(rankedDoc.document(), 1.0 / (RRF_K + rankedDoc.rank())), // Начальная оценка
                        (score1, score2) -> { // Функция слияния для дубликатов
                            score1.add(score2.score());
                            return score1;
                        }
                ));

        // Шаг 2: Сортируем документы по убыванию их итоговой RRF-оценки.
        List<Document> finalDocs = scores.values().stream()
                .sorted(Comparator.comparing(RrfScore::score).reversed())
                .map(RrfScore::document)
                .toList();

        log.info("Слияние результатов завершено. Итоговый список содержит {} уникальных документов.", finalDocs.size());
        return finalDocs;
    }

    /**
     * Вспомогательный record для хранения документа и его ранга.
     */
    private record RankedDocument(Document document, int rank) {
    }

    /**
     * Вспомогательный класс для хранения документа и его RRF-оценки.
     */
    private static class RrfScore {
        private final Document document;
        private double score;

        RrfScore(Document document, double initialScore) {
            this.document = document;
            this.score = initialScore;
        }

        void add(double value) {
            this.score += value;
        }

        Document document() {
            return document;
        }

        double score() {
            return score;
        }
    }
}
