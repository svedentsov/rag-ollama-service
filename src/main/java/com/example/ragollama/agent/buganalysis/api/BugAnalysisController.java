package com.example.ragollama.agent.buganalysis.api;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.buganalysis.api.dto.BugReportSummaryRequest;
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
 * Контроллер для AI-агентов, анализирующих баг-репорты.
 * <p>
 * Предоставляет API для выполнения сложных, многоэтапных бизнес-процессов,
 * таких как структурирование отчета с последующим поиском дубликатов или
 * генерация скрипта для воспроизведения. Вся логика оркестрации делегируется
 * {@link AgentOrchestratorService}.
 */
@RestController
@RequestMapping("/api/v1/agents/bug-analyzer")
@RequiredArgsConstructor
@Tag(name = "Bug Analysis Agent", description = "API для анализа баг-репортов на дубликаты и улучшения качества")
public class BugAnalysisController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Принимает "сырой" текст баг-репорта, запускает конвейер для его
     * структурирования и проверки на дубликаты.
     *
     * @param request DTO с текстом отчета от пользователя.
     * @return {@link Mono} с результатом, содержащим структурированный отчет и список дубликатов.
     */
    @PostMapping("/summarize-and-check")
    @Operation(summary = "Структурировать отчет и проверить на дубликаты",
            description = "Принимает неструктурированный текст, описывающий проблему, использует LLM для " +
                    "его структурирования, а затем по улучшенному тексту ищет дубликаты.")
    public Mono<List<AgentResult>> summarizeAndCheckBugReport(@Valid @RequestBody BugReportSummaryRequest request) {
        AgentContext context = request.toAgentContext();
        return orchestratorService.invoke("bug-report-analysis-pipeline", context);
    }

    /**
     * Принимает "сырой" текст бага и запускает конвейер для генерации
     * исполняемого API-теста для его воспроизведения.
     *
     * @param request DTO с текстом отчета от пользователя.
     * @return {@link Mono} с результатом, содержащим сгенерированный Java-код.
     */
    @PostMapping("/generate-repro-script")
    @Operation(summary = "Сгенерировать скрипт для воспроизведения бага",
            description = "Принимает неструктурированный текст, описывающий проблему, структурирует его " +
                    "и генерирует Java/RestAssured тест, который воспроизводит ошибку.")
    public Mono<List<AgentResult>> generateReproScript(@Valid @RequestBody BugReportSummaryRequest request) {
        AgentContext context = request.toAgentContext();
        return orchestratorService.invoke("bug-reproduction-pipeline", context);
    }
}
