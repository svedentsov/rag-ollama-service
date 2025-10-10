package com.example.ragollama.agent.knowledgegraph.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.knowledgegraph.api.dto.KnowledgeGraphRequest;
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
 * Контроллер для AI-агентов, взаимодействующих с Графом Знаний.
 */
@RestController
@RequestMapping("/api/v1/agents/knowledge")
@RequiredArgsConstructor
@Tag(name = "Knowledge Graph Agents", description = "API для запросов к графу знаний проекта")
public class KnowledgeGraphController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Принимает вопрос на естественном языке, преобразует его в Cypher,
     * выполняет в графе и возвращает человекочитаемый ответ.
     *
     * @param request DTO с вопросом пользователя.
     * @return {@link Mono} с финальным ответом.
     */
    @PostMapping("/query")
    @Operation(summary = "Задать вопрос к графу знаний проекта")
    public Mono<List<AgentResult>> queryKnowledgeGraph(@Valid @RequestBody KnowledgeGraphRequest request) {
        return orchestratorService.invoke("knowledge-aggregator-pipeline", request.toAgentContext());
    }
}
