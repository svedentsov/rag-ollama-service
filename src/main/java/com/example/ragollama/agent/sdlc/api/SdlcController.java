package com.example.ragollama.agent.sdlc.api;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.autonomy.SdlcOrchestratorAgent;
import com.example.ragollama.agent.sdlc.api.dto.SdlcRequest;
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
 * <p>
 * Предоставляет единый, интеллектуальный эндпоинт, который скрывает всю
 * сложность нижележащих агентных конвейеров. Этот API позволяет пользователям
 * взаимодействовать с системой на уровне бизнес-целей, а не технических команд.
 */
@RestController
@RequestMapping("/api/v1/sdlc")
@RequiredArgsConstructor
@Tag(name = "SDLC Orchestrator", description = "Единый языковой интерфейс для управления QA-процессами")
public class SdlcController {

    private final SdlcOrchestratorAgent orchestratorAgent;

    /**
     * Принимает высокоуровневую цель, планирует и выполняет ее, оркеструя
     * запуск необходимых статических конвейеров.
     *
     * @param request DTO с целью и начальным контекстом (например, Git-ссылками).
     * @return {@link Mono} с агрегированными результатами всех выполненных конвейеров.
     */
    @PostMapping("/execute")
    @Operation(summary = "Выполнить высокоуровневую SDLC-задачу",
            description = "Принимает задачу на естественном языке (например, 'провести полный аудит безопасности'). " +
                    "AI-планировщик сам выберет и запустит необходимые конвейеры.")
    public Mono<List<AgentResult>> executeSdlcGoal(@Valid @RequestBody SdlcRequest request) {
        return orchestratorAgent.execute(request.goal(), request.toAgentContext());
    }
}
