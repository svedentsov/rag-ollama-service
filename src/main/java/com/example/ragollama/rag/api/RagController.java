package com.example.ragollama.rag.api;

import com.example.ragollama.orchestration.OrchestrationService;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import com.example.ragollama.shared.task.TaskSubmissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG Controller", description = "Основной API для выполнения RAG-запросов")
public class RagController {

    private final OrchestrationService orchestrationService;

    @PostMapping("/query")
    @Operation(summary = "Задать вопрос к базе знаний (асинхронный запуск)")
    public ResponseEntity<TaskSubmissionResponse> query(@Valid @RequestBody RagQueryRequest request) {
        TaskSubmissionResponse response = orchestrationService.processAsync(request.toUniversalRequest());
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Задать вопрос к базе знаний (потоковый ответ)")
    public Flux<UniversalResponse> queryStream(@Valid @RequestBody RagQueryRequest request) {
        return orchestrationService.processStream(request.toUniversalRequest());
    }
}
