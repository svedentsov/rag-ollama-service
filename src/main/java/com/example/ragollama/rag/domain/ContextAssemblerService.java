package com.example.ragollama.rag.domain;

import com.example.ragollama.rag.strategy.ContextArrangementStrategy;
import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.tokenization.TokenizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для интеллектуальной сборки контекста для RAG-промпта.
 * <p>Этот сервис решает две ключевые задачи:
 * <ol>
 *   <li>Применяет выбранную {@link ContextArrangementStrategy} для оптимального
 *   расположения документов.</li>
 *   <li>Управляет контекстным окном, отбирая столько документов из
 *   упорядоченного списка, сколько помещается в заданный лимит токенов.</li>
 * </ol>
 */
@Service
@Slf4j
public class ContextAssemblerService {

    private final TokenizationService tokenizationService;
    private final ContextArrangementStrategy arrangementStrategy;
    private final int maxContextTokens;

    /**
     * Минимальное количество токенов, которое имеет смысл добавлять при отсечении.
     */
    private static final int MIN_TRUNCATION_TOKENS = 20;

    /**
     * Конструктор для внедрения зависимостей.
     *
     * @param tokenizationService Сервис для работы с токенами.
     * @param arrangementStrategy Активная стратегия для упорядочивания документов (выбирается Spring по property).
     * @param appProperties       Типобезопасная конфигурация приложения.
     */
    public ContextAssemblerService(
            TokenizationService tokenizationService,
            ContextArrangementStrategy arrangementStrategy,
            AppProperties appProperties) {
        this.tokenizationService = tokenizationService;
        this.arrangementStrategy = arrangementStrategy;
        this.maxContextTokens = appProperties.context().maxTokens();
        log.info("ContextAssemblerService инициализирован с лимитом в {} токенов.", maxContextTokens);
    }

    /**
     * Собирает список документов для контекста, не превышая лимит токенов.
     *
     * @param documents Список документов, извлеченных из векторного хранилища.
     * @return Отфильтрованный и потенциально усеченный список документов,
     * готовый для передачи в шаблон промпта.
     */
    public List<Document> assembleContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<Document> arrangedDocs = arrangementStrategy.arrange(documents);
        List<Document> contextDocuments = new ArrayList<>();
        int currentTokenCount = 0;

        // Приблизительная оценка токенов на "обвязку" XML-подобного формата в промпте
        final int perDocumentOverheadTokens = 20;

        for (Document doc : arrangedDocs) {
            String textToUse = (String) doc.getMetadata().getOrDefault("summary", doc.getText());
            int documentTokens = tokenizationService.countTokens(textToUse);
            int requiredTokens = documentTokens + perDocumentOverheadTokens;

            if (currentTokenCount + requiredTokens <= maxContextTokens) {
                contextDocuments.add(doc);
                currentTokenCount += requiredTokens;
            } else {
                int remainingTokens = maxContextTokens - currentTokenCount - perDocumentOverheadTokens;
                if (remainingTokens >= MIN_TRUNCATION_TOKENS) {
                    String truncatedText = tokenizationService.truncate(textToUse, remainingTokens);
                    // Создаем новый документ с усеченным текстом, чтобы не изменять оригинал
                    Document truncatedDoc = new Document(truncatedText, doc.getMetadata());
                    contextDocuments.add(truncatedDoc);
                    log.debug("Последний документ был усечен для экономии места в контексте.");
                }
                log.info("Достигнут лимит контекста. Включено {} из {} документов.", contextDocuments.size(), arrangedDocs.size());
                break; // Прекращаем сборку, так как лимит достигнут
            }
        }

        log.info("Контекст собран из {} документов.", contextDocuments.size());
        return contextDocuments;
    }
}
