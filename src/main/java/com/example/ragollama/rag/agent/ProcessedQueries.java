package com.example.ragollama.rag.agent;

import java.util.List;

/**
 * DTO для структурированного вывода из конвейера обработки запросов.
 * <p>
 * Этот record является неизменяемым и потокобезопасным контейнером,
 * который четко разделяет основной (трансформированный) запрос от
 * дополнительных (расширенных), позволяя строить гибкие стратегии извлечения.
 *
 * @param primaryQuery     Самый релевантный, трансформированный запрос для точного поиска.
 * @param expansionQueries Список альтернативных запросов для повышения полноты.
 */
public record ProcessedQueries(
        String primaryQuery,
        List<String> expansionQueries
) {
}
