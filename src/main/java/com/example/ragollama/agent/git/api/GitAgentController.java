package com.example.ragollama.agent.git.api;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.git.api.dto.GitInspectRequest;
import com.example.ragollama.agent.git.api.dto.ReleaseNotesRequest;
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
import java.util.Map;

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
     * @param request DTO с `oldRef` и `newRef`.
     * @return {@link Mono} с результатом работы агента.
     */
    @PostMapping("/inspect")
    @Operation(summary = "Проанализировать изменения между двумя Git-ссылками",
            description = "Запускает 'git-inspector-pipeline' для получения списка измененных файлов.")
    public Mono<List<AgentResult>> inspectChanges(@Valid @RequestBody GitInspectRequest request) {
        AgentContext context = new AgentContext(Map.of(
                "oldRef", request.oldRef(),
                "newRef", request.newRef()
        ));
        return orchestratorService.invoke("git-inspector-pipeline", context);
    }

    /**
     * Запускает конвейер для поиска пробелов в тестовом покрытии.
     *
     * @param request DTO с `oldRef` и `newRef`.
     * @return {@link Mono} с результатами, включая список пробелов.
     */
    @PostMapping("/analyze-test-gaps")
    @Operation(summary = "Найти пробелы в тестовом покрытии для изменений",
            description = "Запускает 'test-coverage-pipeline', который сначала находит измененные файлы, " +
                    "а затем ищет исходные файлы без соответствующих изменений в тестах.")
    public Mono<List<AgentResult>> analyzeTestGaps(@Valid @RequestBody GitInspectRequest request) {
        AgentContext context = new AgentContext(Map.of(
                "oldRef", request.oldRef(),
                "newRef", request.newRef()
        ));
        return orchestratorService.invoke("test-coverage-pipeline", context);
    }

    /**
     * Запускает полный, глубокий аудит безопасности для изменений в коде.
     *
     * @param request DTO с `oldRef` и `newRef`.
     * @return {@link Mono} с результатами, включая список найденных рисков.
     */
    @PostMapping("/deep-security-audit")
    @Operation(summary = "Провести глубокий аудит безопасности изменений",
            description = "Запускает 'deep-security-audit-pipeline', который находит измененные файлы, " +
                    "извлекает из них правила RBAC и анализирует их на предмет рисков.")
    public Mono<List<AgentResult>> deepSecurityAudit(@Valid @RequestBody GitInspectRequest request) {
        AgentContext context = new AgentContext(Map.of(
                "oldRef", request.oldRef(),
                "newRef", request.newRef()
        ));
        return orchestratorService.invoke("deep-security-audit-pipeline", context);
    }

    /**
     * Запускает конвейер для анализа влияния изменений в коде.
     *
     * @param request DTO с `oldRef` и `newRef`.
     * @return {@link Mono} с отчетом о потенциальном влиянии.
     */
    @PostMapping("/analyze-impact")
    @Operation(summary = "Проанализировать влияние изменений в коде",
            description = "Запускает 'impact-analysis-pipeline', который сначала находит измененные файлы, " +
                    "а затем с помощью LLM прогнозирует их влияние на другие части системы.")
    public Mono<List<AgentResult>> analyzeImpact(@Valid @RequestBody GitInspectRequest request) {
        AgentContext context = new AgentContext(Map.of(
                "oldRef", request.oldRef(),
                "newRef", request.newRef()
        ));
        return orchestratorService.invoke("impact-analysis-pipeline", context);
    }

    /**
     * Запускает полный конвейер для генерации тестов безопасности.
     *
     * @param request DTO с `oldRef` и `newRef`.
     * @return {@link Mono} с результатом, содержащим сгенерированный код тестов.
     */
    @PostMapping("/generate-auth-tests")
    @Operation(summary = "Сгенерировать тесты безопасности для измененных правил",
            description = "Запускает 'auth-test-generation-pipeline', который находит изменения, " +
                    "извлекает правила RBAC и генерирует для них код тестов.")
    public Mono<List<AgentResult>> generateAuthTests(@Valid @RequestBody GitInspectRequest request) {
        AgentContext context = new AgentContext(Map.of(
                "oldRef", request.oldRef(),
                "newRef", request.newRef()
        ));
        return orchestratorService.invoke("auth-test-generation-pipeline", context);
    }

    /**
     * Запускает конвейер для генерации заметок о выпуске.
     *
     * @param request DTO с `oldRef` и `newRef`.
     * @return {@link Mono} с результатом, содержащим Markdown с заметками.
     */
    @PostMapping("/generate-release-notes")
    @Operation(summary = "Сгенерировать заметки о выпуске (release notes)",
            description = "Запускает 'release-notes-generation-pipeline', который анализирует " +
                    "коммиты между двумя Git-ссылками и генерирует из них человекочитаемый отчет.")
    public Mono<List<AgentResult>> generateReleaseNotes(@Valid @RequestBody ReleaseNotesRequest request) {
        AgentContext context = request.toAgentContext();
        return orchestratorService.invoke("release-notes-generation-pipeline", context);
    }
}
