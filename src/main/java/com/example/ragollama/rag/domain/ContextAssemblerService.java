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
 * <p>
 * В этой версии сервис больше не "сплющивает" контекст в строку. Его
 * единственная задача — **управление контекстным окном**. Он отбирает
 * наиболее релевантные документы, которые помещаются в заданный лимит
 * токенов, и возвращает их в виде структурированного списка.
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
     * @param arrangementStrategy Стратегия для упорядочивания документов.
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
