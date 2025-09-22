package com.example.ragollama.ingestion.splitter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Продвинутая стратегия разделения, специализированная для Markdown-документов.
 * <p>
 * Эта стратегия реализует семантический подход, разбивая документ на "родительские"
 * чанки по заголовкам второго уровня (##). Это позволяет сохранить вместе
 * заголовок секции и все относящееся к нему содержимое (параграфы, списки, таблицы),
 * что критически важно для сохранения контекста.
 * <p>
 * `TextSplitterService` затем использует эти родительские чанки для создания
 * более мелких, дочерних чанков для индексации.
 */
@Slf4j
@Component
@Order(20) // Приоритет выше, чем у RecursiveTextSplitterStrategy, но ниже чем у JavaCodeSplitterStrategy
public class HierarchicalMarkdownSplitterStrategy implements DocumentSplitterStrategy {

    // Паттерн для поиска заголовков 2-го уровня (## Title) в начале строки
    private static final Pattern H2_HEADER_PATTERN = Pattern.compile("(?m)^##\\s.*$");

    /**
     * {@inheritDoc}
     * <p>
     * Считаем документ подходящим, если он пришел из Confluence, так как
     * это наш основной источник Markdown-документации.
     *
     * @param document Документ для проверки.
     * @return {@code true}, если источник документа - Confluence.
     */
    @Override
    public boolean supports(Document document) {
        Object source = document.getMetadata().get("source");
        // Эта эвристика специфична для данного проекта
        return source instanceof String && ((String) source).toLowerCase().contains("confluence");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Разбивает весь Markdown-документ на крупные семантические блоки (родительские чанки),
     * где каждый блок начинается с заголовка `##`.
     *
     * @param document Исходный Markdown-документ.
     * @param config   Конфигурация (в данной реализации не используется).
     * @return Список родительских чанков.
     */
    @Override
    public List<Document> split(Document document, SplitterConfig config) {
        log.debug("Применение HierarchicalMarkdownSplitterStrategy для документа: {}", document.getMetadata().get("source"));
        final String text = document.getText();
        List<Document> parentChunks = new ArrayList<>();
        Matcher matcher = H2_HEADER_PATTERN.matcher(text);

        int lastEnd = 0;
        // Находим все вхождения заголовков
        while (matcher.find()) {
            // Текст между предыдущим заголовком (или началом файла) и текущим
            if (matcher.start() > lastEnd) {
                String chunkText = text.substring(lastEnd, matcher.start()).trim();
                if (!chunkText.isEmpty()) {
                    parentChunks.add(new Document(chunkText, document.getMetadata()));
                }
            }
            lastEnd = matcher.start();
        }

        // Добавляем оставшийся текст после последнего заголовка
        if (lastEnd < text.length()) {
            String chunkText = text.substring(lastEnd).trim();
            if (!chunkText.isEmpty()) {
                parentChunks.add(new Document(chunkText, document.getMetadata()));
            }
        }

        // Если заголовки не найдены вообще, возвращаем весь документ как один родительский чанк
        if (parentChunks.isEmpty() && !text.isBlank()) {
            parentChunks.add(document);
        }

        log.debug("Markdown-документ разделен на {} родительских чанков.", parentChunks.size());
        return parentChunks;
    }
}
