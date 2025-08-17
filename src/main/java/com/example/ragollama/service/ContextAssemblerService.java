package com.example.ragollama.service;

import com.example.ragollama.config.properties.AppProperties;
import com.example.ragollama.rag.strategy.ContextArrangementStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

/**
 * Сервис для интеллектуальной сборки контекста для RAG-промпта.
 * <p>
 * Эта версия использует паттерн "Стратегия" для переупорядочивания
 * документов и реализует механизм "умного" отсечения (precision truncation).
 * Если последний релевантный документ не помещается целиком, сервис попытается
 * включить его начало, обрезанное точно по оставшемуся лимиту токенов.
 */
@Service
@Slf4j
public class ContextAssemblerService {

    private final TokenizationService tokenizationService;
    private final ContextArrangementStrategy arrangementStrategy;
    private final int maxContextTokens;

    // Минимальное количество токенов, которое имеет смысл добавлять при отсечении.
    // Предотвращает добавление бессмысленных обрывков из 1-2 слов.
    private static final int MIN_TRUNCATION_TOKENS = 20;

    public ContextAssemblerService(
            TokenizationService tokenizationService,
            ContextArrangementStrategy arrangementStrategy,
            AppProperties appProperties) {
        this.tokenizationService = tokenizationService;
        this.arrangementStrategy = arrangementStrategy;
        this.maxContextTokens = appProperties.context().maxTokens();
        log.info("ContextAssemblerService инициализирован с лимитом в {} токенов и умным отсечением.", maxContextTokens);
    }

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
            String docText = doc.getText();
            int documentTokens = tokenizationService.countTokens(docText);

            int requiredTokens = documentTokens + (documentCount > 0 ? separatorTokens : 0);

            if (currentTokenCount + requiredTokens <= maxContextTokens) {
                // Документ помещается целиком
                contextJoiner.add(docText);
                currentTokenCount += requiredTokens;
                documentCount++;
            } else {
                // Документ не помещается целиком, пытаемся добавить его часть
                int remainingTokens = maxContextTokens - currentTokenCount - (documentCount > 0 ? separatorTokens : 0);
                if (remainingTokens >= MIN_TRUNCATION_TOKENS) {
                    String truncatedText = tokenizationService.truncate(docText, remainingTokens);
                    contextJoiner.add(truncatedText);
                    currentTokenCount += tokenizationService.countTokens(truncatedText) + (documentCount > 0 ? separatorTokens : 0);
                    documentCount++;
                    log.debug("Последний документ был усечен, чтобы поместиться в контекст. Добавлено ~{} токенов.", remainingTokens);
                }
                // Если места осталось слишком мало, просто прекращаем сборку
                log.debug("Достигнут лимит контекста ({}/{} токенов). Добавлено {} из {} документов. Пропускаем остальные.",
                        currentTokenCount, maxContextTokens, arrangedDocs.size());
                break;
            }
        }

        String finalContext = contextJoiner.toString();
        log.info("Контекст собран из {} документов, итоговый размер: {} токенов.", documentCount, tokenizationService.countTokens(finalContext));
        return finalContext;
    }
}
