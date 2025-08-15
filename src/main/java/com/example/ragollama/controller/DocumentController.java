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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Controller", description = "API для загрузки и индексации документов")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    @Operation(
            summary = "Поставить документ в очередь на индексацию",
            description = "Принимает документ и ставит его в очередь на фоновую обработку. Возвращает ID задачи.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Документ принят в обработку"),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос")})
    public ResponseEntity<DocumentResponse> scheduleDocumentUpload(@Valid @RequestBody DocumentRequest documentRequest) {
        UUID jobId = documentService.scheduleDocumentIngestion(documentRequest);
        DocumentResponse response = new DocumentResponse(jobId.toString(), 0);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }
}
