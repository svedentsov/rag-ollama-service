package com.example.ragollama.ingestion.api;

import com.example.ragollama.ingestion.api.dto.DocumentIngestionRequest;
import com.example.ragollama.ingestion.api.dto.JobSubmissionResponse;
import com.example.ragollama.ingestion.domain.DocumentIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

/**
 * Контроллер для управления документами.
 * Предоставляет API для асинхронной загрузки и постановки документов в очередь на индексацию.
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Controller", description = "API для загрузки и индексации документов")
public class DocumentIngestionController {

    private final DocumentIngestionService documentIngestionService;

    /**
     * Принимает документ и ставит его в очередь на фоновую обработку.
     * Этот эндпоинт работает асинхронно. Он немедленно возвращает ответ
     * с кодом 202 (Accepted) и идентификатором созданной задачи. Клиент может
     * использовать этот ID для отслеживания статуса обработки через другие эндпоинты.
     *
     * @param documentIngestionRequest DTO с текстом и метаданными документа.
     * @return {@link ResponseEntity} с {@link JobSubmissionResponse}, содержащим ID задачи.
     */
    @PostMapping
    @Operation(
            summary = "Поставить документ в очередь на индексацию",
            description = "Принимает документ, создает задачу на его обработку и немедленно возвращает ID этой задачи.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Документ принят в обработку. В теле ответа содержится ID задачи.",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = JobSubmissionResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос (например, пустой текст)")})
    public ResponseEntity<JobSubmissionResponse> scheduleDocumentIngestion(@Valid @RequestBody DocumentIngestionRequest documentIngestionRequest) {
        UUID jobId = documentIngestionService.scheduleDocumentIngestion(documentIngestionRequest);
        JobSubmissionResponse response = new JobSubmissionResponse(jobId);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }
}
