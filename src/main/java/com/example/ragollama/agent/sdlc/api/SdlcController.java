package com.example.ragollama.agent.sdlc.api;

import com.example.ragollama.agent.autonomy.AutonomousGoalExecutor;
import com.example.ragollama.agent.sdlc.api.dto.SdlcRequest;
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
 * Контроллер для высокоуровневого, языкового управления жизненным циклом разработки.
 */
@RestController
@RequestMapping("/api/v1/sdlc")
@RequiredArgsConstructor
@Tag(name = "SDLC Orchestrator", description = "Единый языковой интерфейс для управления QA-процессами")
public class SdlcController {

    private final AutonomousGoalExecutor goalExecutor;

    /**
     * Принимает высокоуровневую цель, планирует и выполняет ее.
     *
     * @param request DTO с целью и начальным контекстом (например, Git-ссылками).
     * @return {@link CompletableFuture} с агрегированными результатами.
     */
    @PostMapping("/execute")
    @Operation(summary = "Выполнить высокоуровневую SDLC-задачу",
            description = "Принимает задачу на естественном языке (например, 'провести полный аудит безопасности'). " +
                    "AI-планировщик сам выберет и запустит необходимые инструменты или конвейеры.")
    public CompletableFuture<Void> executeSdlcGoal(@Valid @RequestBody SdlcRequest request) {
        return CompletableFuture.runAsync(() ->
                goalExecutor.executeGoal(request.goal(), request.toAgentContext())
        );
    }
}
