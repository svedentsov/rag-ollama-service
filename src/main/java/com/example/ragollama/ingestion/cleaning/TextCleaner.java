package com.example.ragollama.ingestion.cleaning;

/**
 * Функциональный интерфейс для реализации стратегий очистки текста.
 * Каждая реализация отвечает за удаление одного конкретного типа "шума".
 */
@FunctionalInterface
public interface TextCleaner {
    /**
     * Применяет правило очистки к входному тексту.
     *
     * @param text Исходный "грязный" текст.
     * @return Очищенный текст.
     */
    String clean(String text);
}
