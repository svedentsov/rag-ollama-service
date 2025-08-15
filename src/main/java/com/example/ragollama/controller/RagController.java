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
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG Controller", description = "API для выполнения RAG (Retrieval-Augmented Generation) запросов")
public class RagController {

    private final RagService ragService;

    @PostMapping("/query")
    @Operation(
            summary = "Задать вопрос на основе загруженных документов (асинхронно)",
            description = "Выполняет поиск релевантной информации в векторном хранилище, а затем передает ее вместе с вопросом в LLM для генерации ответа. Запрос обрабатывается неблокирующим способом.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ от AI"),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
                    @ApiResponse(responseCode = "422", description = "Обнаружена попытка Prompt Injection"),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера при обработке запроса")
            })
    public CompletableFuture<ResponseEntity<RagQueryResponse>> queryRag(@Valid @RequestBody RagQueryRequest ragQueryRequest) {
        return ragService.queryAsync(ragQueryRequest)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Произошла ошибка при асинхронной обработке RAG-запроса: '{}'", ragQueryRequest.query(), ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    /**
     * Выполняет RAG-запрос и возвращает ответ по частям в реальном времени через Server-Sent Events.
     */
    @PostMapping(path = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Задать вопрос и получить потоковый ответ (SSE)",
            description = "Выполняет RAG-запрос и возвращает ответ по частям в реальном времени через Server-Sent Events.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Потоковая передача ответа началась"),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
                    @ApiResponse(responseCode = "422", description = "Обнаружена попытка Prompt Injection")
            })
    public Flux<ServerSentEvent<String>> queryRagStream(@Valid @RequestBody RagQueryRequest ragQueryRequest) {
        Flux<String> tokenStream = ragService.queryStream(ragQueryRequest);
        return tokenStream
                .map(token -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .event("message")
                        .data(token)
                        .build())
                .doOnSubscribe(subscription -> log.info("SSE-соединение установлено для запроса: '{}'", ragQueryRequest.query()))
                .doOnError(error -> {
                    // ClientAbortException - это нормальная ситуация, когда клиент закрывает соединение.
                    // Не логируем это как ERROR, чтобы не засорять логи.
                    if (error instanceof ClientAbortException) {
                        log.warn("Клиент разорвал SSE-соединение для запроса: '{}'. Это ожидаемое поведение.", ragQueryRequest.query());
                    } else {
                        // Все остальные ошибки - это реальные проблемы на сервере.
                        log.error("Произошла ошибка в стриме RAG-ответа для запроса: '{}'", ragQueryRequest.query(), error);
                    }
                })
                .doOnComplete(() -> log.info("SSE-соединение штатно завершено для запроса: '{}'", ragQueryRequest.query()))
                .mergeWith(Flux.interval(Duration.ofSeconds(15))
                        .map(i -> ServerSentEvent.<String>builder()
                                .event("ping")
                                .comment("Keep-alive")
                                .build()));
    }
}
