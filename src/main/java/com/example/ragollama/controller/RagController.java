package com.example.ragollama.controller;

import com.example.ragollama.dto.RagQueryRequest;
import com.example.ragollama.dto.RagQueryResponse;
import com.example.ragollama.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для выполнения RAG-запросов.
 */
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG Controller", description = "API для выполнения RAG (Retrieval-Augmented Generation) запросов")
public class RagController {

    private final RagService ragService;

    @PostMapping("/query")
    @Operation(
            summary = "Задать вопрос на основе загруженных документов",
            description = "Выполняет поиск релевантной информации в векторном хранилище, а затем передает ее вместе с вопросом в LLM для генерации ответа.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ от AI"),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
                    @ApiResponse(responseCode = "422", description = "Обнаружена попытка Prompt Injection")})
    public ResponseEntity<RagQueryResponse> queryRag(@Valid @RequestBody RagQueryRequest ragQueryRequest) {
        RagQueryResponse response = ragService.query(ragQueryRequest);
        return ResponseEntity.ok(response);
    }
}
