package com.example.ragollama.rag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Контекстный объект-"контейнер", который передается по цепочке RAG-советников (Advisors).
 * Он агрегирует все данные, необходимые для принятия решений и модификации
 * на каждом этапе обогащения (augmentation). Использование единого контекстного
 * объекта упрощает сигнатуры методов и позволяет гибко добавлять новые
 * данные в конвейер без изменения интерфейсов.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class RagContext {

    /**
     * Оригинальный, немодифицированный запрос пользователя.
     */
    private final String originalQuery;

    /**
     * Список документов, извлеченных из векторной базы.
     * Этот список может быть отфильтрован или изменен советниками.
     */
    private List<Document> documents;

    /**
     * Карта для построения модели финального промпта.
     * Советники могут добавлять в эту карту любые переменные
     * (например, 'current_date'), которые затем будут доступны в шаблоне промпта.
     */
    private final Map<String, Object> promptModel = new ConcurrentHashMap<>();
}
