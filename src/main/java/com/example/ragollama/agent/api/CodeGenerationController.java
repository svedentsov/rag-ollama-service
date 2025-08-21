package com.example.ragollama.agent.api;

import com.example.ragollama.agent.api.dto.CodeGenerationRequest;
import com.example.ragollama.agent.api.dto.CodeGenerationResponse;
import com.example.ragollama.agent.domain.CodeGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для управления AI-агентами-исполнителями.
 */
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
@Tag(name = "Agent Controller", description = "API для взаимодействия с AI-агентами")
public class CodeGenerationController {

    private final CodeGenerationService codeGenerationService;

    /**
     * Принимает задачу на генерацию кода и возвращает результат.
     *
     * @param request DTO с инструкцией и контекстом.
     * @return {@link CompletableFuture} с сгенерированным кодом.
     */
    @PostMapping("/generate-code")
    @Operation(summary = "Сгенерировать код (например, API-тест)",
            description = "Асинхронно генерирует фрагмент кода на основе инструкции и контекста.")
    public CompletableFuture<CodeGenerationResponse> generateCode(@Valid @RequestBody CodeGenerationRequest request) {
        return codeGenerationService.generateCode(request);
    }
}
