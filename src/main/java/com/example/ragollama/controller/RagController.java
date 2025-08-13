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
 * REST-контроллер для выполнения запросов по технологии Retrieval-Augmented Generation (RAG).
 * <p>
 * Этот контроллер является центральной точкой для взаимодействия с RAG-системой.
 */
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG Controller", description = "API для выполнения RAG (Retrieval-Augmented Generation) запросов")
public class RagController {

    private final RagService ragService;

    /**
     * Выполняет RAG-запрос: находит релевантную информацию и генерирует ответ.
     * <p>
     * Процесс состоит из двух этапов:
     * 1. **Retrieval**: Поиск наиболее релевантных документов (чанков) в векторном хранилище по запросу пользователя.
     * 2. **Generation**: Отправка найденного контекста вместе с исходным вопросом в LLM для генерации осмысленного ответа.
     *
     * @param ragQueryRequest DTO с вопросом пользователя и параметрами поиска.
     * @return {@link ResponseEntity} с {@link RagQueryResponse}, содержащим сгенерированный ответ и ссылки на источники.
     */
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
