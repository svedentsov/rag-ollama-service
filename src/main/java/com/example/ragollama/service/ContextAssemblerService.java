package com.example.ragollama.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

/**
 * Сервис для интеллектуальной сборки контекста для RAG-промпта.
 * <p>
 * Его основная задача - собрать строку контекста из списка документов,
 * убедившись, что итоговый размер не превышает заданный лимит по токенам.
 * Использует {@link TokenizationService} для точного подсчета, что
 * предотвращает переполнение контекстного окна LLM и обеспечивает
 * максимальное использование доступного пространства.
 */
@Service
@Slf4j
public class ContextAssemblerService {

    private final TokenizationService tokenizationService;
    private final int maxContextTokens;

    // Оставляем этот явный конструктор. Он необходим для @Value.
    public ContextAssemblerService(TokenizationService tokenizationService,
                                   @Value("${app.context.max-tokens}") int maxContextTokens) {
        this.tokenizationService = tokenizationService;
        this.maxContextTokens = maxContextTokens;
        log.info("ContextAssemblerService инициализирован с лимитом в {} токенов.", maxContextTokens);
    }

    /**
     * Собирает контекст из списка документов, точно соблюдая лимит по токенам.
     * <p>
     * Итеративно проходит по документам (которые должны быть предварительно
     * отсортированы по релевантности) и добавляет их содержимое в контекст,
     * пока не будет достигнут предел по токенам.
     *
     * @param documents Список документов-кандидатов для включения в контекст.
     * @return Финальная строка контекста, готовая для вставки в промпт.
     */
    public String assembleContext(List<Document> documents) {
        StringJoiner contextJoiner = new StringJoiner("\n---\n");
        int currentTokenCount = 0;
        int documentCount = 0;

        // Токены для разделителя "\n---\n"
        final int separatorTokens = tokenizationService.countTokens("\n---\n");

        for (Document doc : documents) {
            int documentTokens = tokenizationService.countTokens(doc.getText());
            int potentialTotalTokens = currentTokenCount + documentTokens;
            if (documentCount > 0) { // Добавляем токены разделителя для всех, кроме первого документа
                potentialTotalTokens += separatorTokens;
            }

            if (potentialTotalTokens <= maxContextTokens) {
                contextJoiner.add(doc.getText());
                currentTokenCount = potentialTotalTokens;
                documentCount++;
            } else {
                log.debug("Достигнут лимит контекста ({}/{} токенов). Добавлено {} из {} документов. Пропускаем остальные.",
                        currentTokenCount, maxContextTokens, documentCount, documents.size());
                break; // Прерываем цикл, так как лимит достигнут
            }
        }

        String finalContext = contextJoiner.toString();
        log.info("Контекст собран из {} документов, итоговый размер: {} токенов.", documentCount, tokenizationService.countTokens(finalContext));
        return finalContext;
    }
}