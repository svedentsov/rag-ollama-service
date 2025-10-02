package com.example.ragollama.chat.api;

import com.example.ragollama.chat.api.dto.ChatRequest;
import com.example.ragollama.orchestration.OrchestrationService;
import com.example.ragollama.orchestration.dto.UniversalResponse;
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
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat Controller", description = "API для простого чата (без RAG)")
public class ChatController {

    private final OrchestrationService orchestrationService;

    @PostMapping
    @Operation(summary = "Отправить сообщение в чат (асинхронный запуск)")
    public ResponseEntity<TaskSubmissionResponse> chat(@Valid @RequestBody ChatRequest request) {
        TaskSubmissionResponse response = orchestrationService.processAsync(request.toUniversalRequest());
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Отправить сообщение в чат (потоковый ответ)")
    public Flux<UniversalResponse> chatStream(@Valid @RequestBody ChatRequest request) {
        return orchestrationService.processStream(request.toUniversalRequest());
    }
}
