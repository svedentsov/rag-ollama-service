package com.example.ragollama.rag.api;

import com.example.ragollama.rag.api.dto.RagQueryRequest;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для выполнения RAG (Retrieval-Augmented Generation) запросов.
 * Предоставляет эндпоинты для выполнения RAG-запросов, которые обогащают
 * знания LLM данными из векторной базы. Поддерживает как асинхронный
 * ответ с полным результатом, так и потоковую передачу данных.
 */
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG Controller", description = "API для выполнения RAG (Retrieval-Augmented Generation) запросов")
public class RagController {

    private final RagService ragService;

    /**
     * Выполняет RAG-запрос и возвращает полный ответ после его генерации.
     *
     * @param ragQueryRequest DTO с запросом и параметрами поиска.
     * @return {@link CompletableFuture} с полным ответом {@link RagQueryResponse}.
     */
    @PostMapping("/query")
    @Operation(summary = "Задать вопрос на основе загруженных документов (асинхронно)",
            description = "Выполняет RAG-запрос и возвращает полный сгенерированный ответ. Запрос обрабатывается неблокирующим способом.")
    public CompletableFuture<RagQueryResponse> queryRag(@Valid @RequestBody RagQueryRequest ragQueryRequest) {
        return ragService.queryAsync(ragQueryRequest);
    }

    /**
     * Выполняет RAG-запрос и возвращает ответ в виде структурированного потока (Server-Sent Events).
     * Поток содержит события разных типов (content, sources, done, error),
     * что позволяет клиенту надежно обрабатывать ответ.
     *
     * @param ragQueryRequest DTO с запросом и параметрами поиска.
     * @return Реактивный поток {@link Flux} со структурированными событиями {@link StreamingResponsePart}.
     */
    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Задать вопрос в потоковом режиме (Streaming/SSE)",
            description = "Выполняет RAG-запрос и возвращает ответ от LLM в виде структурированного потока Server-Sent Events.")
    public Flux<StreamingResponsePart> queryRagStream(@Valid @RequestBody RagQueryRequest ragQueryRequest) {
        return ragService.queryStream(ragQueryRequest);
    }
}
