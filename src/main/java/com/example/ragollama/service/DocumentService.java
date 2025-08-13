package com.example.ragollama.service;

import com.example.ragollama.dto.DocumentRequest;
import com.example.ragollama.dto.DocumentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Сервис для управления документами в векторном хранилище.
 * <p>
 * Отвечает за загрузку, обработку (разбиение на чанки) и индексацию
 * текстовых документов для последующего использования в RAG-запросах.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    /**
     * Векторное хранилище для сохранения и поиска эмбеддингов документов.
     */
    private final VectorStore vectorStore;

    /**
     * Инструмент для разбиения текста на чанки на основе токенов,
     * что более эффективно для LLM, чем простое разбиение по символам.
     */
    private final TokenTextSplitter tokenTextSplitter;

    /**
     * Обрабатывает и сохраняет документ в векторное хранилище.
     * <p>
     * Процесс включает следующие шаги:
     * 1. Генерация уникального ID для исходного документа.
     * 2. Создание объекта {@link Document} с текстом и метаданными (имя источника).
     * 3. Применение {@link TokenTextSplitter} для разбиения документа на чанки.
     *    Это необходимо для эффективной работы с векторным хранилищем и контекстом LLM.
     * 4. Добавление чанков в {@link VectorStore}, где для каждого чанка будет
     *    создан и сохранен эмбеддинг.
     *
     * @param request DTO с текстом документа и его названием.
     * @return DTO с информацией о количестве загруженных чанков.
     */
    @Transactional
    public DocumentResponse processAndStoreDocument(DocumentRequest request) {
        log.info("Processing document with source name: '{}'", request.sourceName());

        String documentId = UUID.randomUUID().toString();
        Document document = new Document(
                request.text(),
                Map.of("source", request.sourceName(), "documentId", documentId)
        );

        // Разбиваем документ на чанки с помощью сплиттера
        List<Document> chunks = tokenTextSplitter.apply(List.of(document));
        log.info("Document split into {} chunks.", chunks.size());

        // Добавляем чанки в векторное хранилище. На этом этапе Spring AI
        // автоматически вызовет модель для создания эмбеддингов.
        vectorStore.add(chunks);
        log.info("Successfully added {} chunks to the vector store.", chunks.size());

        return new DocumentResponse(documentId, chunks.size());
    }
}
