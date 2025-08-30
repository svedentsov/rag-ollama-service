package com.example.ragollama.rag.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация {@link ContextArrangementStrategy}, которая не вносит
 * никаких изменений в порядок документов.
 * <p>
 * Эта стратегия используется по умолчанию или когда требуется передать
 * в LLM документы в том порядке, в котором их вернул этап поиска/реранжирования
 * (т.е. строго по убыванию релевантности).
 * <p>
 * Активируется при {@code app.rag.arrangement-strategy=passthrough} или если
 * свойство не задано.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.rag.arrangement-strategy", havingValue = "passthrough", matchIfMissing = true)
public class PassthroughArrangementStrategy implements ContextArrangementStrategy {

    /**
     * Конструктор, логирующий активацию стратегии.
     */
    public PassthroughArrangementStrategy() {
        log.info("Активирована стратегия компоновки контекста: PassthroughArrangementStrategy (по умолчанию)");
    }

    /**
     * Возвращает исходный список документов без изменений.
     *
     * @param documents Исходный список документов.
     * @return Тот же самый список документов.
     */
    @Override
    public List<Document> arrange(List<Document> documents) {
        log.debug("Стратегия 'passthrough' применена. Порядок документов не изменен.");
        return documents;
    }
}
