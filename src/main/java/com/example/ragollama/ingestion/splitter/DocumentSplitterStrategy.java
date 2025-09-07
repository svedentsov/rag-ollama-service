package com.example.ragollama.ingestion.splitter;

import com.example.ragollama.ingestion.TextSplitterService;
import org.springframework.ai.document.Document;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * Определяет контракт для стратегий разделения одного документа на несколько
 * семантически связанных чанков.
 *
 * <p>Эта стратегия является ключевым элементом для создания контекстно-зависимого
 * чанкинга. Каждая реализация отвечает за один тип контента (например, Java-код,
 * Markdown, обычный текст) и должна быть аннотирована {@link org.springframework.core.annotation.Order}
 * для определения приоритета ее применения в {@link TextSplitterService}.
 */
public interface DocumentSplitterStrategy extends Ordered {

    /**
     * Проверяет, применима ли данная стратегия к предоставленному документу.
     *
     * @param document Документ для проверки, содержащий как текст, так и метаданные.
     * @return {@code true}, если стратегия может обработать этот тип документа, иначе {@code false}.
     */
    boolean supports(Document document);

    /**
     * Разделяет исходный документ на список чанков.
     *
     * <p>Каждый возвращенный документ-чанк должен наследовать метаданные
     * родительского документа. Стратегия также может добавлять специфичные
     * метаданные (например, имя метода для Java-кода).
     *
     * @param document Исходный документ для разделения.
     * @param config   Параметры, управляющие процессом разделения (размер чанка и т.д.).
     * @return Список документов-чанков.
     */
    List<Document> split(Document document, SplitterConfig config);

    /**
     * Возвращает порядок применения стратегии. Стратегии с меньшим значением
     * (более высоким приоритетом) будут проверяться первыми.
     *
     * @return Целочисленное значение порядка.
     */
    @Override
    default int getOrder() {
        return 0;
    }
}
