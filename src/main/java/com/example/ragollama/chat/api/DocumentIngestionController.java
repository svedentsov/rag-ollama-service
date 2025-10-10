package com.example.ragollama.chat.api;

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
import reactor.core.publisher.Mono;


/**
 * Контроллер для управления документами, адаптированный для WebFlux.
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Controller", description = "API для загрузки и индексации документов")
public class DocumentIngestionController {

    private final DocumentIngestionService documentIngestionService;

    /**
     * Принимает документ и асинхронно запускает его обработку.
     *
     * @param documentIngestionRequest DTO с данными документа.
     * @return {@link Mono<ResponseEntity>} с ID задачи.
     */
    @PostMapping
    @Operation(
            summary = "Запустить асинхронную индексацию документа",
            description = "Принимает документ, создает задачу на его обработку и немедленно возвращает ID этой задачи.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Документ принят в обработку.",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = JobSubmissionResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос")})
    public Mono<ResponseEntity<JobSubmissionResponse>> scheduleDocumentIngestion(@Valid @RequestBody com.example.ragollama.ingestion.api.dto.DocumentIngestionRequest documentIngestionRequest) {
        return documentIngestionService.scheduleDocumentIngestion(documentIngestionRequest)
                .map(jobId -> new ResponseEntity<>(new JobSubmissionResponse(jobId), HttpStatus.ACCEPTED));
    }
}
