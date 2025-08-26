package com.example.ragollama.qaagent.api;

import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.api.dto.SdlcRequest;
import com.example.ragollama.qaagent.autonomy.SdlcOrchestratorAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Контроллер для высокоуровневого, языкового управления жизненным циклом разработки.
 */
@RestController
@RequestMapping("/api/v1/sdlc")
@RequiredArgsConstructor
@Tag(name = "SDLC Orchestrator", description = "Единый языковой интерфейс для управления QA-процессами")
public class SdlcController {

    private final SdlcOrchestratorAgent orchestratorAgent;

    /**
     * Принимает высокоуровневую цель, планирует и выполняет ее.
     *
     * @param request DTO с целью и начальным контекстом.
     * @return {@link Mono} с агрегированными результатами всех выполненных конвейеров.
     */
    @PostMapping("/execute")
    @Operation(summary = "Выполнить высокоуровневую SDLC-задачу")
    public Mono<List<AgentResult>> executeSdlcGoal(@Valid @RequestBody SdlcRequest request) {
        return orchestratorAgent.execute(request.goal(), request.toAgentContext());
    }
}
