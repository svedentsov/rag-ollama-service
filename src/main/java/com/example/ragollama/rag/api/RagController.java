package com.example.ragollama.rag.api;

import com.example.ragollama.orchestration.RagApplicationService;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
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
 * Контроллер, предоставляющий API для RAG-взаимодействия.
 * <p>
 * Эталонная реализация контроллера в Clean Architecture. Он является тонким
 * слоем, который принимает HTTP-запросы, валидирует DTO и делегирует всю
 * работу специализированному {@link RagApplicationService}.
 *
 * @see RagApplicationService
 */
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG Controller", description = "Основной API для выполнения RAG-запросов")
public class RagController {

    private final RagApplicationService ragApplicationService;

    /**
     * Принимает запрос, выполняет полный RAG-конвейер и возвращает
     * сгенерированный ответ вместе с источниками.
     *
     * @param request DTO с вопросом пользователя и параметрами поиска.
     * @return {@link CompletableFuture} с {@link RagQueryResponse}.
     */
    @PostMapping("/query")
    @Operation(summary = "Задать вопрос к базе знаний (полный ответ)")
    public CompletableFuture<RagQueryResponse> query(@Valid @RequestBody RagQueryRequest request) {
        return ragApplicationService.processRagRequestAsync(request);
    }

    /**
     * Принимает запрос и возвращает ответ в виде потока Server-Sent Events (SSE).
     *
     * @param request DTO с вопросом пользователя и параметрами поиска.
     * @return Реактивный поток {@link Flux}, передающий структурированные части ответа.
     */
    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Задать вопрос к базе знаний (потоковый ответ)")
    public Flux<StreamingResponsePart> queryStream(@Valid @RequestBody RagQueryRequest request) {
        return ragApplicationService.processRagRequestStream(request);
    }
}
