package com.example.ragollama.orchestration.api;

import com.example.ragollama.orchestration.OrchestrationService;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@RestController
@RequestMapping("/api/v1/orchestrator")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Universal AI Orchestrator", description = "Единый API для взаимодействия с системой (RAG, Chat, Agents)")
public class UniversalController {

    private final OrchestrationService orchestrationService;
    private final ObjectMapper objectMapper; // Добавляем ObjectMapper для сериализации

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
     * @return Реактивный поток {@link Flux} со строками в формате SSE.
     */
    @PostMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Универсальный потоковый эндпоинт (SSE)")
    public Flux<String> askStream(@Valid @RequestBody UniversalRequest request) {
        return orchestrationService.processStream(request)
                .map(this::formatAsSse);
    }

    /**
     * Сериализует объект ответа в строку и форматирует ее в соответствии
     * со спецификацией Server-Sent Events.
     *
     * @param response Объект для отправки.
     * @return Строка в формате "data: {...}\n\n".
     */
    private String formatAsSse(UniversalResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            return "data: " + json + "\n\n";
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать SSE-событие в JSON", e);
            try {
                String errorJson = objectMapper.writeValueAsString(new UniversalResponse.Error("Ошибка сериализации на сервере"));
                return "data: " + errorJson + "\n\n";
            } catch (JsonProcessingException ex) {
                return "data: {\"type\":\"error\",\"message\":\"Критическая ошибка сериализации\"}\n\n";
            }
        }
    }
}
