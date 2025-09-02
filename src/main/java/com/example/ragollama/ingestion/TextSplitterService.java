package com.example.ragollama.ingestion;

import com.example.ragollama.ingestion.splitter.RecursiveTextSplitterStrategy;
import com.example.ragollama.ingestion.splitter.SplitterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис-оркестратор для интеллектуального разбиения текста на чанки.
 * <p>
 * Эта версия реализует паттерн "Стратегия". Она не содержит логики
 * разделения сама, а делегирует эту задачу конкретной реализации
 * {@link com.example.ragollama.ingestion.splitter.SplitterStrategy}.
 * Это делает сервис гибким и расширяемым.
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
     * @return Список документов-чанков.
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
     * @return Список документов-чанков.
     */
    public List<Document> split(Document document, SplitterConfig config) {
        log.debug("Запуск разделения документа '{}' со стратегией '{}'",
                document.getMetadata().get("source"), recursiveStrategy.getClass().getSimpleName());

        List<String> stringChunks = recursiveStrategy.split(document.getText(), config);

        return stringChunks.stream()
                .map(chunkText -> new Document(chunkText, document.getMetadata()))
                .collect(Collectors.toList());
    }
}
