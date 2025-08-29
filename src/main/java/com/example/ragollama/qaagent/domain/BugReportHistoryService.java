package com.example.ragollama.qaagent.domain;

import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Сервис для извлечения истории баг-репортов из базы знаний.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BugReportHistoryService {

    private final HybridRetrievalStrategy retrievalStrategy;

    /**
     * Извлекает все документы типа 'bug_report' из векторного хранилища.
     * <p>
     * Примечание: В реальной системе с большим объемом данных здесь бы
     * использовалась пагинация или более сложные методы выборки. Для
     * демонстрации мы извлекаем до 1000 последних отчетов.
     *
     * @return {@link Mono} со списком документов.
     */
    public Mono<List<Document>> fetchAllBugReports() {
        log.info("Извлечение всех баг-репортов из базы знаний для анализа паттернов...");
        Filter.Expression filter = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("metadata.doc_type"),
                new Filter.Value("bug_report")
        );
        // Используем "пустой" RAG-запрос с фильтром, чтобы получить все документы нужного типа
        return retrievalStrategy.retrieve(null, "*", 1000, 0.0, filter);
    }
}
