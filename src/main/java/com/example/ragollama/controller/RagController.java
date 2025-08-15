package com.example.ragollama.controller;

import com.example.ragollama.dto.RagQueryRequest;
import com.example.ragollama.dto.RagQueryResponse;
import com.example.ragollama.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

@Slf4j
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
     * @return CompletableFuture с полным ответом.
     */
    @PostMapping("/query")
    @Operation(
            summary = "Задать вопрос на основе загруженных документов (асинхронно)",
            description = "Выполняет RAG-запрос и возвращает полный сгенерированный ответ. Запрос обрабатывается неблокирующим способом.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ от AI"),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
                    @ApiResponse(responseCode = "422", description = "Обнаружена попытка Prompt Injection"),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера при обработке запроса")})
    public CompletableFuture<ResponseEntity<RagQueryResponse>> queryRag(@Valid @RequestBody RagQueryRequest ragQueryRequest) {
        return ragService.queryAsync(ragQueryRequest)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Произошла ошибка при асинхронной обработке RAG-запроса: '{}'", ragQueryRequest.query(), ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    /**
     * Выполняет RAG-запрос и возвращает ответ в виде потока (Server-Sent Events).
     *
     * @param ragQueryRequest DTO с запросом и параметрами поиска.
     * @return Реактивный поток {@link Flux}, передающий части ответа по мере их генерации.
     */
    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Задать вопрос в потоковом режиме (Streaming/SSE)",
            description = "Выполняет RAG-запрос и возвращает ответ от LLM в виде потока Server-Sent Events. Ответ генерируется и отправляется по частям, что значительно улучшает UX.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Поток успешно открыт"),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
                    @ApiResponse(responseCode = "422", description = "Обнаружена попытка Prompt Injection")})
    public Flux<String> queryRagStream(@Valid @RequestBody RagQueryRequest ragQueryRequest) {
        return ragService.queryStream(ragQueryRequest);
    }
}
