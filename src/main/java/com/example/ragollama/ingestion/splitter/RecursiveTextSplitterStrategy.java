package com.example.ragollama.ingestion.splitter;

import com.example.ragollama.shared.tokenization.TokenizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Реализация {@link DocumentSplitterStrategy}, использующая рекурсивный подход
 * к разделению текста.
 * <p>
 * Стратегия последовательно применяет разделители из {@link SplitterConfig}
 * (от крупных к мелким), пытаясь создать чанки, которые максимально близки
 * к целевому размеру, не разрывая при этом семантически связанные части текста.
 * <p>
 * Эта стратегия имеет самый низкий приоритет и выступает в роли
 * **fallback-механизма** для всех типов документов, для которых не нашлось
 * более специфичной стратегии (например, для простого текста).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class RecursiveTextSplitterStrategy implements DocumentSplitterStrategy {

    private final TokenizationService tokenizationService;

    /**
     * {@inheritDoc}
     *
     * @param document Документ для проверки.
     * @return Всегда {@code true}, так как это fallback-стратегия.
     */
    @Override
    public boolean supports(Document document) {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @param document Исходный документ для разделения.
     * @param config   Параметры, управляющие процессом разделения.
     * @return Список документов-чанков.
     */
    @Override
    public List<Document> split(Document document, SplitterConfig config) {
        if (document.getText() == null || document.getText().isBlank()) {
            return Collections.emptyList();
        }
        List<String> textChunks = splitRecursively(List.of(document.getText()), config.delimiters(), config);

        return textChunks.stream()
                .map(chunkText -> new Document(chunkText, document.getMetadata()))
                .toList();
    }

    /**
     * Рекурсивно разделяет текст, применяя список разделителей.
     *
     * @param texts      Текущий список фрагментов текста для обработки.
     * @param delimiters Оставшийся список разделителей.
     * @param config     Общая конфигурация чанкинга.
     * @return Список финальных чанков.
     */
    private List<String> splitRecursively(List<String> texts, List<String> delimiters, SplitterConfig config) {
        if (texts.isEmpty()) {
            return Collections.emptyList();
        }

        int chunkSize = config.chunkSize();
        List<String> finalChunks = new ArrayList<>();

        for (String text : texts) {
            if (tokenizationService.countTokens(text) <= chunkSize) {
                finalChunks.add(text);
            } else {
                if (delimiters.isEmpty()) {
                    finalChunks.addAll(forceSplit(text, chunkSize));
                } else {
                    String nextDelimiter = delimiters.getFirst();
                    List<String> subTexts = Arrays.asList(text.split(nextDelimiter));
                    List<String> remainingDelimiters = delimiters.subList(1, delimiters.size());
                    finalChunks.addAll(splitRecursively(subTexts, remainingDelimiters, config));
                }
            }
        }
        return mergeChunks(finalChunks, config);
    }

    /**
     * Объединяет мелкие фрагменты в чанки, не превышающие целевой размер.
     *
     * @param fragments Список мелких фрагментов.
     * @param config    Конфигурация чанкинга.
     * @return Список объединенных чанков.
     */
    private List<String> mergeChunks(List<String> fragments, SplitterConfig config) {
        List<String> merged = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        List<String> sentenceBuffer = new ArrayList<>(); // Буфер для управления пересечением

        for (String fragment : fragments) {
            if (fragment.isBlank()) continue;
            int fragmentTokens = tokenizationService.countTokens(fragment);
            int currentTokens = tokenizationService.countTokens(currentChunk.toString());

            if (currentTokens + fragmentTokens > config.chunkSize() && !currentChunk.isEmpty()) {
                merged.add(currentChunk.toString().trim());
                sentenceBuffer = getOverlap(sentenceBuffer, config);
                currentChunk = new StringBuilder(String.join(" ", sentenceBuffer));
            }
            currentChunk.append(" ").append(fragment);
            sentenceBuffer.add(fragment);
        }

        if (!currentChunk.isEmpty()) {
            merged.add(currentChunk.toString().trim());
        }
        return merged;
    }

    private List<String> getOverlap(List<String> buffer, SplitterConfig config) {
        if (buffer.isEmpty() || config.chunkOverlap() <= 0) {
            return new ArrayList<>();
        }
        int currentOverlapTokens = 0;
        int startIndex = -1;
        for (int i = buffer.size() - 1; i >= 0; i--) {
            String sentence = buffer.get(i);
            currentOverlapTokens += tokenizationService.countTokens(sentence);
            if (currentOverlapTokens > config.chunkOverlap()) {
                break;
            }
            startIndex = i;
        }
        return (startIndex == -1) ? new ArrayList<>() : new ArrayList<>(buffer.subList(startIndex, buffer.size()));
    }

    private List<String> forceSplit(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String remainingText = text;
        while (tokenizationService.countTokens(remainingText) > chunkSize) {
            String chunk = tokenizationService.truncate(remainingText, chunkSize);
            chunks.add(chunk);
            remainingText = remainingText.substring(chunk.length());
        }
        if (!remainingText.isBlank()) {
            chunks.add(remainingText);
        }
        return chunks;
    }
}
