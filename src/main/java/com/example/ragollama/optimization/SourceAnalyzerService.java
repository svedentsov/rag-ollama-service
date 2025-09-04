package com.example.ragollama.optimization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Детерминированный сервис для анализа метаданных источников (документов).
 * <p>
 * Выполняет объективные, основанные на правилах, вычисления для оценки
 * таких качеств, как актуальность и авторитетность, предоставляя числовые
 * оценки для дальнейшего использования в Trust Score.
 */
@Slf4j
@Service
public class SourceAnalyzerService {

    /**
     * Карта авторитетности источников. Ключ - префикс имени источника,
     * значение - оценка от 0 до 100.
     */
    private static final Map<String, Integer> AUTHORITY_MAP = Map.of(
            "Confluence-Policy", 100,
            "Confluence", 80,
            "JIRA", 60,
            "test_case", 50
    );
    private static final int DEFAULT_AUTHORITY = 70;

    /**
     * Анализирует список документов и вычисляет среднюю оценку их актуальности.
     *
     * @param documents Список документов из RAG-контекста.
     * @return Средняя оценка актуальности (0-100).
     */
    public int analyzeRecency(List<Document> documents) {
        if (documents == null || documents.isEmpty()) return 0;

        double totalScore = documents.stream()
                .mapToLong(this::getRecencyScoreForDocument)
                .average()
                .orElse(0.0);

        return (int) totalScore;
    }

    /**
     * Анализирует список документов и вычисляет среднюю оценку их авторитетности.
     *
     * @param documents Список документов из RAG-контекста.
     * @return Средняя оценка авторитетности (0-100).
     */
    public int analyzeAuthority(List<Document> documents) {
        if (documents == null || documents.isEmpty()) return 0;

        double averageAuthority = documents.stream()
                .mapToInt(this::getAuthorityScoreForDocument)
                .average()
                .orElse((double) DEFAULT_AUTHORITY);

        return (int) averageAuthority;
    }

    /**
     * Вычисляет оценку актуальности для одного документа на основе его временной метки.
     *
     * @param doc Документ для анализа.
     * @return Оценка от 0 до 100.
     */
    private long getRecencyScoreForDocument(Document doc) {
        try {
            Object timestampObj = doc.getMetadata().get("last_modified");
            if (timestampObj instanceof String timestampStr) {
                long daysOld = ChronoUnit.DAYS.between(OffsetDateTime.parse(timestampStr), OffsetDateTime.now());
                if (daysOld <= 7) return 100; // Последняя неделя
                if (daysOld <= 30) return 90; // Последний месяц
                if (daysOld <= 180) return 70; // Последние полгода
                if (daysOld <= 365) return 50; // Последний год
                return 20; // Очень старый
            }
        } catch (Exception e) {
            log.trace("Не удалось определить новизну документа ID {}: {}", doc.getId(), e.getMessage());
        }
        return 60; // Средняя оценка по умолчанию
    }

    /**
     * Вычисляет оценку авторитетности для одного документа на основе его источника.
     *
     * @param doc Документ для анализа.
     * @return Оценка от 0 до 100.
     */
    private int getAuthorityScoreForDocument(Document doc) {
        String source = (String) doc.getMetadata().getOrDefault("source", "");
        return AUTHORITY_MAP.entrySet().stream()
                .filter(entry -> source.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(DEFAULT_AUTHORITY);
    }
}
