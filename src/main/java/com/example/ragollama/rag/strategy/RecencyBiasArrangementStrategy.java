package com.example.ragollama.rag.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Реализация {@link ContextArrangementStrategy}, которая переупорядочивает
 * документы, отдавая приоритет более свежим данным.
 * <p>
 * Эта стратегия полезна в сценариях, где актуальность информации
 * является ключевым фактором. Она ожидает, что в метаданных каждого
 * документа будет поле "timestamp" с датой в формате ISO 8601.
 * Документы без валидной временной метки считаются наименее приоритетными.
 * <p>
 * Активируется при {@code app.rag.arrangement-strategy=recency}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.rag.arrangement-strategy", havingValue = "recency")
public class RecencyBiasArrangementStrategy implements ContextArrangementStrategy {

    /**
     * Ключ в метаданных, по которому ищется временная метка.
     */
    public static final String TIMESTAMP_METADATA_KEY = "timestamp";

    /**
     * Конструктор, логирующий активацию стратегии.
     */
    public RecencyBiasArrangementStrategy() {
        log.info("Активирована стратегия компоновки контекста: RecencyBiasArrangementStrategy");
    }

    /**
     * Переупорядочивает список документов, сортируя их по убыванию
     * временной метки из метаданных.
     *
     * @param documents Исходный список документов.
     * @return Новый список, отсортированный по дате (от новых к старым).
     */
    @Override
    public List<Document> arrange(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<Document> sortedDocs = documents.stream()
                .sorted(Comparator.comparing(this::extractTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        log.debug("Документы пересортированы с учетом новизны. Исходный размер: {}, итоговый: {}.",
                documents.size(), sortedDocs.size());
        return sortedDocs;
    }

    /**
     * Безопасно извлекает и парсит временную метку из метаданных документа.
     *
     * @param doc Документ для анализа.
     * @return Объект {@link OffsetDateTime} в случае успеха, иначе {@code null}.
     */
    private OffsetDateTime extractTimestamp(Document doc) {
        try {
            return Objects.toString(doc.getMetadata().get(TIMESTAMP_METADATA_KEY), null)
                    .transform(timestampStr -> timestampStr != null ? OffsetDateTime.parse(timestampStr) : null);
        } catch (DateTimeParseException e) {
            log.warn("Не удалось распарсить timestamp для документа ID '{}'. Документ будет иметь низкий приоритет.", doc.getId());
            return null;
        }
    }
}
