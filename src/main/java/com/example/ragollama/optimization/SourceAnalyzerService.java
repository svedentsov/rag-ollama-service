package com.example.ragollama.optimization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SourceAnalyzerService {

    private static final Map<String, Integer> AUTHORITY_MAP = Map.of(
            "Confluence-Policy", 100,
            "Confluence", 80,
            "JIRA", 60,
            "test_case", 50
    );
    private static final int DEFAULT_AUTHORITY = 70;

    public int analyzeRecency(List<Document> documents) {
        if (documents == null || documents.isEmpty()) return 0;

        double totalScore = documents.stream()
                .mapToLong(this::getRecencyScoreForDocument)
                .average()
                .orElse(0.0);

        return (int) totalScore;
    }

    public int analyzeAuthority(List<Document> documents) {
        if (documents == null || documents.isEmpty()) return 0;

        double averageAuthority = documents.stream()
                .mapToInt(this::getAuthorityScoreForDocument)
                .average()
                .orElse((double) DEFAULT_AUTHORITY);

        return (int) averageAuthority;
    }

    private long getRecencyScoreForDocument(Document doc) {
        try {
            Object timestampObj = doc.getMetadata().get("timestamp"); // Предполагаем наличие такого поля
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

    private int getAuthorityScoreForDocument(Document doc) {
        String source = (String) doc.getMetadata().getOrDefault("source", "");
        return AUTHORITY_MAP.entrySet().stream()
                .filter(entry -> source.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(DEFAULT_AUTHORITY);
    }
}
