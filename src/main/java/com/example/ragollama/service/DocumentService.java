package com.example.ragollama.service;

import com.example.ragollama.dto.DocumentRequest;
import com.example.ragollama.dto.DocumentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для обработки и сохранения документов в векторном хранилище.
 * <p>
 * Отвечает за основной этап "Ingestion" в RAG-архитектуре:
 * получение текста, его разделение на чанки, векторизацию и сохранение.
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
     * Синхронно обрабатывает и сохраняет документ.
     * <p>
     * {@link CacheEvict} используется для очистки кэша результатов поиска. Это необходимо,
     * так как добавление нового документа может изменить результаты будущих поисковых запросов.
     *
     * @param request DTO с данными документа.
     * @return {@link DocumentResponse} с результатом обработки.
     */
    @Transactional
    @CacheEvict(value = "vector_search_results", allEntries = true)
    public DocumentResponse processAndStoreDocument(DocumentRequest request) {
        log.info("Синхронная обработка документа: '{}'", request.sourceName());
        return ingestDocument(request);
    }

    /**
     * Асинхронно обрабатывает и сохраняет документ.
     * <p>
     * Метод помечен аннотацией {@link Async}, что позволяет выполнять его в отдельном потоке,
     * не блокируя вызывающий поток (например, HTTP-обработчик).
     *
     * @param request DTO с данными документа.
     * @return {@link CompletableFuture} с {@link DocumentResponse} после завершения.
     */
    @Async
    @Transactional
    @CacheEvict(value = "vector_search_results", allEntries = true)
    public CompletableFuture<DocumentResponse> processAndStoreDocumentAsync(DocumentRequest request) {
        log.info("Асинхронная обработка документа: '{}'", request.sourceName());
        return CompletableFuture.completedFuture(ingestDocument(request));
    }

    /**
     * Внутренний метод, реализующий логику индексации документа.
     *
     * @param request DTO с данными документа.
     * @return Результат обработки.
     */
    private DocumentResponse ingestDocument(DocumentRequest request) {
        // Создаем уникальный ID для всего документа
        String documentId = UUID.randomUUID().toString();

        // Создаем объект Document из Spring AI, добавляя метаданные
        Document document = new Document(
                request.text(),
                Map.of("source", request.sourceName(), "documentId", documentId)
        );

        // Разбиваем документ на чанки с помощью TokenTextSplitter
        List<Document> chunks = tokenTextSplitter.apply(List.of(document));
        log.info("Документ '{}' разделен на {} чанков.", request.sourceName(), chunks.size());

        // Добавляем чанки в векторное хранилище, где они будут векторизованы и сохранены
        vectorStore.add(chunks);
        log.info("Успешно добавлено {} чанков в векторное хранилище.", chunks.size());

        return new DocumentResponse(documentId, chunks.size());
    }
}
