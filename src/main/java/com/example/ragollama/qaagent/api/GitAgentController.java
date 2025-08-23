package com.example.ragollama.qaagent.api;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentOrchestratorService;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.api.dto.GitInspectRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для управления AI-агентами, взаимодействующими с Git.
 */
@RestController
@RequestMapping("/api/v1/agents/git")
@RequiredArgsConstructor
@Tag(name = "Git Agents", description = "API для запуска AI-агентов для анализа Git-репозиториев")
public class GitAgentController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает агента для анализа изменений между двумя Git-ссылками.
     *
     * @param request DTO с `oldRef` и `newRef` (коммиты, ветки, теги).
     * @return {@link CompletableFuture} с результатом работы агента.
     */
    @PostMapping("/inspect")
    @Operation(summary = "Проанализировать изменения между двумя Git-ссылками",
            description = "Запускает 'git-inspector-pipeline' для получения списка измененных файлов.")
    public CompletableFuture<List<AgentResult>> inspectChanges(@Valid @RequestBody GitInspectRequest request) {
        AgentContext context = new AgentContext(Map.of(
                "oldRef", request.oldRef(),
                "newRef", request.newRef()
        ));
        return orchestratorService.invokePipeline("git-inspector-pipeline", context);
    }
}
