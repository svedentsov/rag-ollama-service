package com.example.ragollama.controller;

import com.example.ragollama.dto.DocumentRequest;
import com.example.ragollama.dto.DocumentResponse;
import com.example.ragollama.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер для управления документами в векторном хранилище.
 * <p>
 * Предоставляет API для загрузки и индексации текстовых документов,
 * которые будут использоваться в RAG-сценариях.
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Controller", description = "API для загрузки и индексации документов")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Принимает текстовый документ, обрабатывает и сохраняет его в векторное хранилище.
     * <p>
     * Процесс включает разбиение текста на чанки, создание для каждого чанка
     * векторного представления (эмбеддинга) и сохранение в базе данных PgVector.
     *
     * @param documentRequest DTO с исходным именем и текстом документа.
     * @return {@link ResponseEntity} со статусом 201 (CREATED) и {@link DocumentResponse},
     * содержащим ID документа и количество созданных чанков.
     */
    @PostMapping
    @Operation(
            summary = "Загрузить и проиндексировать документ",
            description = "Принимает текст документа, разбивает его на чанки, создает эмбеддинги и сохраняет их в векторное хранилище.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Документ успешно обработан и сохранен"),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос (например, пустой текст документа)")})
    public ResponseEntity<DocumentResponse> uploadDocument(@Valid @RequestBody DocumentRequest documentRequest) {
        DocumentResponse response = documentService.processAndStoreDocument(documentRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
