package com.example.ragollama.ingestion;

import com.example.ragollama.shared.tokenization.TokenizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Кастомный сервис для интеллектуального разбиения текста на чанки.
 * <p>
 * Реализует рекурсивную стратегию, которая сохраняет семантическую
 * целостность, разделяя текст сначала по параграфам, затем по предложениям.
 * Это дает полный контроль над процессом чанкинга и не зависит от
 * конкретной реализации в Spring AI.
 */
@Service
@RequiredArgsConstructor
public class TextSplitterService {

    private final TokenizationService tokenizationService;
    private final IngestionProperties ingestionProperties;

    // Регулярное выражение для разделения на предложения, учитывающее точки, восклицательные и вопросительные знаки.
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("(?<=[.!?])\\s+");

    /**
     * Разбивает один документ на список чанков.
     *
     * @param document Документ для обработки.
     * @return Список документов-чанков.
     */
    public List<Document> split(Document document) {
        int chunkSize = ingestionProperties.chunking().defaultChunkSize();
        int chunkOverlap = ingestionProperties.chunking().chunkOverlap();
        List<Document> chunks = new ArrayList<>();

        // Сначала делим по параграфам
        String[] paragraphs = document.getText().split("\n\n");
        List<String> currentChunkSentences = new ArrayList<>();
        int currentChunkTokens = 0;

        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) continue;

            String[] sentences = SENTENCE_SPLIT_PATTERN.split(paragraph);
            for (String sentence : sentences) {
                int sentenceTokens = tokenizationService.countTokens(sentence);
                if (currentChunkTokens + sentenceTokens > chunkSize && !currentChunkSentences.isEmpty()) {
                    // Завершаем текущий чанк
                    chunks.add(createChunk(currentChunkSentences, document));
                    // Начинаем новый чанк с пересечением
                    currentChunkSentences = getOverlap(currentChunkSentences, chunkOverlap);
                    currentChunkTokens = currentChunkSentences.stream().mapToInt(tokenizationService::countTokens).sum();
                }
                currentChunkSentences.add(sentence);
                currentChunkTokens += sentenceTokens;
            }
        }

        // Добавляем последний оставшийся чанк
        if (!currentChunkSentences.isEmpty()) {
            chunks.add(createChunk(currentChunkSentences, document));
        }

        return chunks;
    }

    private Document createChunk(List<String> sentences, Document originalDocument) {
        String chunkText = String.join(" ", sentences).trim();
        return new Document(chunkText, originalDocument.getMetadata());
    }

    private List<String> getOverlap(List<String> sentences, int overlapTokens) {
        if (sentences.isEmpty() || overlapTokens <= 0) {
            return new ArrayList<>();
        }

        List<String> overlapSentences = new ArrayList<>();
        int currentOverlapTokens = 0;
        for (int i = sentences.size() - 1; i >= 0; i--) {
            String sentence = sentences.get(i);
            int sentenceTokens = tokenizationService.countTokens(sentence);
            if (currentOverlapTokens + sentenceTokens > overlapTokens) {
                break;
            }
            overlapSentences.add(0, sentence);
            currentOverlapTokens += sentenceTokens;
        }
        return overlapSentences;
    }
}
