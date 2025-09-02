package com.example.ragollama.ingestion.splitter;

import com.example.ragollama.shared.tokenization.TokenizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Реализация {@link SplitterStrategy}, использующая рекурсивный подход
 * к разделению текста.
 * <p>
 * Стратегия последовательно применяет разделители из {@link SplitterConfig}
 * (от крупных к мелким), пытаясь создать чанки, которые максимально близки
 * к целевому размеру, не разрывая при этом семантически связанные части текста.
 */
@Component
@RequiredArgsConstructor
public class RecursiveTextSplitterStrategy implements SplitterStrategy {

    private final TokenizationService tokenizationService;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> split(String text, SplitterConfig config) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return splitRecursively(List.of(text), config.delimiters(), config);
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
                    // Достигли последнего уровня рекурсии, но текст все еще слишком большой.
                    // Принудительно делим его на части.
                    finalChunks.addAll(forceSplit(text, chunkSize));
                } else {
                    // Рекурсивный шаг: делим по следующему разделителю.
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
                // Начинаем новый чанк с пересечением
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

    /**
     * Оптимально получает пересечение из буфера предложений.
     *
     * @param buffer Буфер с фрагментами текста последнего чанка.
     * @param config Конфигурация.
     * @return Список фрагментов для пересечения.
     */
    private List<String> getOverlap(List<String> buffer, SplitterConfig config) {
        if (buffer.isEmpty() || config.chunkOverlap() <= 0) {
            return new ArrayList<>();
        }
        int currentOverlapTokens = 0;
        int startIndex = -1;
        // Идем с конца, чтобы найти начальный индекс для subList
        for (int i = buffer.size() - 1; i >= 0; i--) {
            String sentence = buffer.get(i);
            currentOverlapTokens += tokenizationService.countTokens(sentence);
            if (currentOverlapTokens > config.chunkOverlap()) {
                break;
            }
            startIndex = i;
        }
        if (startIndex == -1) {
            return new ArrayList<>();
        }
        // subList работает очень эффективно (O(1))
        return new ArrayList<>(buffer.subList(startIndex, buffer.size()));
    }


    /**
     * Принудительно делит текст, если он превышает размер чанка даже после всех разделителей.
     */
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
