package com.example.ragollama.ingestion;

import com.example.ragollama.ingestion.splitter.RecursiveTextSplitterStrategy;
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
 *
 * <p>Эта версия реализует паттерн "Стратегия" и обогащает каждый
 * созданный чанк уникальным, трассируемым идентификатором в формате
 * {@code {original_document_id}:{chunk_index}}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TextSplitterService {

    private final IngestionProperties ingestionProperties;
    private final RecursiveTextSplitterStrategy recursiveStrategy;

    private static final List<String> DEFAULT_DELIMITERS = List.of(
            "\n\n", // Параграфы
            "\n",   // Новые строки
            "(?<=[.!?])\\s+" // Предложения
    );

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
     * Разбивает один документ на список чанков, используя кастомную конфигурацию.
     *
     * @param document Документ для обработки.
     * @param config   Кастомные параметры чанкинга.
     * @return Список документов-чанков, каждый с уникальным ID.
     */
    public List<Document> split(Document document, SplitterConfig config) {
        log.debug("Запуск разделения документа '{}' со стратегией '{}'",
                document.getMetadata().get("source"), recursiveStrategy.getClass().getSimpleName());

        List<String> stringChunks = recursiveStrategy.split(document.getText(), config);
        String originalDocId = (String) document.getMetadata().get("documentId");
        AtomicInteger chunkCounter = new AtomicInteger(0);

        return stringChunks.stream()
                .map(chunkText -> {
                    // Создаем новый документ-чанк, наследуя метаданные
                    Document chunkDoc = new Document(chunkText, document.getMetadata());
                    // Генерируем и добавляем уникальный, трассируемый ID для чанка
                    String chunkId = String.format("%s:%d", originalDocId, chunkCounter.getAndIncrement());
                    chunkDoc.getMetadata().put("chunkId", chunkId);
                    return chunkDoc;
                })
                .collect(Collectors.toList());
    }
}
