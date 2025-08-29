package com.example.ragollama.rag.agent;

import java.util.List;

/**
 * record для структурированного вывода из конвейера.
 *
 * @param primaryQuery     Самый релевантный, трансформированный запрос для точного поиска.
 * @param expansionQueries Список альтернативных запросов для повышения полноты.
 */
public record ProcessedQueries(String primaryQuery, List<String> expansionQueries) {
}
