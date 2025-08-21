package com.example.ragollama.ingestion.cleaning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис, оркестрирующий процесс очистки текста перед индексацией.
 * Применяет цепочку всех доступных стратегий {@link TextCleaner}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataCleaningService {

    private final List<TextCleaner> cleaners;

    /**
     * Последовательно применяет все зарегистрированные очистители к тексту.
     *
     * @param rawText Исходный текст документа.
     * @return Полностью очищенный текст.
     */
    public String cleanDocumentText(String rawText) {
        String cleanedText = rawText;
        for (TextCleaner cleaner : cleaners) {
            cleanedText = cleaner.clean(cleanedText);
        }
        return cleanedText;
    }
}
