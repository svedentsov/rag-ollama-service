package com.example.ragollama.orchestration.api;

import com.example.ragollama.orchestration.OrchestrationService;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * Универсальный контроллер, являющийся единой точкой входа для всех
 * интерактивных запросов к AI-системе.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orchestrator")
@RequiredArgsConstructor
@Tag(name = "Universal AI Orchestrator", description = "Единый API для взаимодействия с системой (RAG, Chat, Agents)")
public class UniversalController {

    private final OrchestrationService orchestrationService;

    /**
     * Обрабатывает любой пользовательский запрос и возвращает полный ответ после его генерации.
     *
     * @param request Унифицированный DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с полным, агрегированным ответом {@link UniversalSyncResponse}.
     */
    @PostMapping("/ask")
    @Operation(summary = "Универсальный синхронный эндпоинт (полный ответ)")
    public CompletableFuture<UniversalSyncResponse> ask(@Valid @RequestBody UniversalRequest request) {
        return orchestrationService.processSync(request);
    }

    /**
     * Обрабатывает любой пользовательский запрос в потоковом режиме (Server-Sent Events).
     *
     * @param request Унифицированный DTO с запросом от пользователя.
     * @return Реактивный поток UniversalResponse. Spring автоматически преобразует каждый
     *         элемент в строку "data: {...}\n\n".
     */
    @PostMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Универсальный потоковый эндпоинт (SSE)")
    public Flux<UniversalResponse> askStream(@Valid @RequestBody UniversalRequest request) {
        return orchestrationService.processStream(request);
    }
}
