package com.example.ragollama.rag.domain;

import com.example.ragollama.rag.strategy.ContextArrangementStrategy;
import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.tokenization.TokenizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

/**
 * Сервис для интеллектуальной сборки контекста для RAG-промпта.
 * <p>
 * Эта версия реализует стратегию "summary-first". Она сначала пытается
 * использовать краткое содержание (summary) из метаданных документа,
 * которое было сгенерировано на этапе индексации. Если summary отсутствует,
 * используется полный текст документа. Это позволяет передавать в LLM
 * более концентрированный и релевантный контекст.
 * <p>
 * Также используется паттерн "Стратегия" для переупорядочивания
 * документов и механизм "умного" отсечения (precision truncation) для
 * максимального заполнения контекстного окна.
 */
@Service
@Slf4j
public class ContextAssemblerService {

    private final TokenizationService tokenizationService;
    private final ContextArrangementStrategy arrangementStrategy;
    private final int maxContextTokens;

    /**
     * Минимальное количество токенов, которое имеет смысл добавлять при отсечении.
     * Предотвращает добавление бессмысленных обрывков из 1-2 слов.
     */
    private static final int MIN_TRUNCATION_TOKENS = 20;

    /**
     * Конструктор для внедрения зависимостей.
     *
     * @param tokenizationService Сервис для работы с токенами.
     * @param arrangementStrategy Стратегия для упорядочивания документов перед сборкой.
     * @param appProperties       Типобезопасная конфигурация приложения.
     */
    public ContextAssemblerService(
            TokenizationService tokenizationService,
            ContextArrangementStrategy arrangementStrategy,
            AppProperties appProperties) {
        this.tokenizationService = tokenizationService;
        this.arrangementStrategy = arrangementStrategy;
        this.maxContextTokens = appProperties.context().maxTokens();
        log.info("ContextAssemblerService инициализирован с лимитом в {} токенов и стратегией 'summary-first'.", maxContextTokens);
    }

    /**
     * Собирает единую строку контекста из списка документов, не превышая лимит токенов.
     *
     * @param documents Список документов, извлеченных из векторного хранилища.
     * @return Строка, содержащая объединенный и оптимизированный контекст.
     */
    public String assembleContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        List<Document> arrangedDocs = arrangementStrategy.arrange(documents);

        StringJoiner contextJoiner = new StringJoiner("\n---\n");
        int currentTokenCount = 0;
        int documentCount = 0;

        final int separatorTokens = tokenizationService.countTokens("\n---\n");

        for (Document doc : arrangedDocs) {
            // ПРИОРИТЕТ: Используем summary, если оно есть, иначе - полный текст.
            String textToUse = (String) doc.getMetadata().getOrDefault("summary", doc.getText());
            int documentTokens = tokenizationService.countTokens(textToUse);

            int requiredTokens = documentTokens + (documentCount > 0 ? separatorTokens : 0);

            if (currentTokenCount + requiredTokens <= maxContextTokens) {
                // Документ помещается целиком
                contextJoiner.add(textToUse);
                currentTokenCount += requiredTokens;
                documentCount++;
            } else {
                // Документ не помещается целиком, пытаемся добавить его часть
                int remainingTokens = maxContextTokens - currentTokenCount - (documentCount > 0 ? separatorTokens : 0);
                if (remainingTokens >= MIN_TRUNCATION_TOKENS) {
                    String truncatedText = tokenizationService.truncate(textToUse, remainingTokens);
                    contextJoiner.add(truncatedText);
                    currentTokenCount += tokenizationService.countTokens(truncatedText) + (documentCount > 0 ? separatorTokens : 0);
                    documentCount++;
                    log.debug("Последний документ был усечен, чтобы поместиться в контекст. Добавлено ~{} токенов.", remainingTokens);
                }
                // Если места осталось слишком мало, просто прекращаем сборку
                log.debug("Достигнут лимит контекста ({}/{} токенов). Добавлено {} из {} документов. Пропускаем остальные.",
                        currentTokenCount, maxContextTokens, documentCount, arrangedDocs.size());
                break;
            }
        }

        String finalContext = contextJoiner.toString();
        log.info("Контекст собран из {} документов, итоговый размер: {} токенов.", documentCount, tokenizationService.countTokens(finalContext));
        return finalContext;
    }
}
