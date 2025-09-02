package com.example.ragollama.ingestion.splitter;

import java.util.List;

/**
 * Функциональный интерфейс, определяющий контракт для стратегий разделения текста.
 * <p>
 * Каждая реализация инкапсулирует один конкретный алгоритм чанкинга
 * (например, рекурсивный, по маркерам Markdown, по методам в коде).
 */
@FunctionalInterface
public interface SplitterStrategy {

    /**
     * Разделяет исходный текст на список строковых чанков в соответствии
     * с логикой стратегии и предоставленной конфигурацией.
     *
     * @param text   Исходный текст для разделения.
     * @param config Параметры, управляющие процессом разделения (размер чанка, пересечение и т.д.).
     * @return Список строковых чанков, готовых для дальнейшего преобразования в {@link org.springframework.ai.document.Document}.
     */
    List<String> split(String text, SplitterConfig config);
}
