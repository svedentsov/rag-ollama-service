package com.example.ragollama.ingestion;

import com.example.ragollama.ingestion.splitter.DocumentSplitterStrategy;
import com.example.ragollama.ingestion.splitter.SplitterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Сервис-оркестратор для интеллектуального разбиения текста на чанки.
 * <p>
 * Эта версия реализует паттерн "Стратегия". Он содержит список всех
 * доступных реализаций {@link DocumentSplitterStrategy}, отсортированных
 * по приоритету. Для каждого документа он находит первую подходящую
 * стратегию и делегирует ей задачу разделения.
 * <p>
 * Сервис также отвечает за централизованное обогащение каждого
 * созданного чанка уникальным, трассируемым идентификатором в формате
 * {@code {original_document_id}:{chunk_index}}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TextSplitterService {

    private final IngestionProperties ingestionProperties;
    private final List<DocumentSplitterStrategy> strategies;

    private static final List<String> DEFAULT_DELIMITERS = List.of("\n\n", "\n", "(?<=[.!?])\\s+");

    /**
     * Разбивает один документ на список чанков, используя конфигурацию по умолчанию.
     *
     * @param document Документ для обработки.
     * @return Список документов-чанков, каждый с уникальным ID.
     */
    public List<Document> split(Document document) {
        SplitterConfig defaultConfig = new SplitterConfig(
                ingestionProperties.chunking().defaultChunkSize(),
                ingestionProperties.chunking().chunkOverlap(),
                DEFAULT_DELIMITERS
        );
        return split(document, defaultConfig);
    }

    /**
     * Разбивает один документ на список чанков, выбирая наиболее подходящую
     * стратегию и используя кастомную конфигурацию.
     *
     * @param document Документ для обработки.
     * @param config   Кастомные параметры чанкинга.
     * @return Список документов-чанков, каждый с уникальным ID.
     */
    public List<Document> split(Document document, SplitterConfig config) {
        // Находим первую стратегию, которая поддерживает данный тип документа.
        // Spring гарантирует, что список `strategies` отсортирован по @Order.
        DocumentSplitterStrategy strategy = strategies.stream()
                .filter(s -> s.supports(document))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Не найдена подходящая стратегия для документа. " +
                        "Необходимо иметь как минимум одну fallback-стратегию."));

        log.debug("Выбрана стратегия '{}' для документа '{}'",
                strategy.getClass().getSimpleName(), document.getMetadata().get("source"));

        List<Document> chunks = strategy.split(document, config);
        String originalDocId = (String) document.getMetadata().get("documentId");
        AtomicInteger chunkCounter = new AtomicInteger(0);

        // Централизованно присваиваем ID каждому чанку.
        return chunks.stream()
                .map(chunkDoc -> {
                    String chunkId = String.format("%s:%d", originalDocId, chunkCounter.getAndIncrement());
                    chunkDoc.getMetadata().put("chunkId", chunkId);
                    return chunkDoc;
                })
                .collect(Collectors.toList());
    }
}
