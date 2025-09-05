package com.example.ragollama.agent.buganalysis.api;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.buganalysis.api.dto.BugAnalysisRequest;
import com.example.ragollama.agent.buganalysis.api.dto.BugAnalysisResponse;
import com.example.ragollama.agent.buganalysis.api.dto.BugReportSummaryRequest;
import com.example.ragollama.agent.buganalysis.domain.BugAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для AI-агента, анализирующего баг-репорты.
 */
@RestController
@RequestMapping("/api/v1/agents/bug-analyzer")
@RequiredArgsConstructor
@Tag(name = "Bug Analysis Agent", description = "API для анализа баг-репортов на дубликаты и улучшения качества")
public class BugAnalysisController {

    private final BugAnalysisService bugAnalysisService;
    private final AgentOrchestratorService orchestratorService;

    /**
     * Асинхронно анализирует черновик баг-репорта, ищет дубликаты и предлагает улучшенное описание.
     *
     * @param request DTO с текстом черновика отчета.
     * @return {@link CompletableFuture} со структурированным результатом анализа.
     * Ответ будет отправлен клиенту после асинхронного завершения операции.
     */
    @PostMapping
    @Operation(summary = "Проанализировать баг-репорт",
            description = "Принимает черновик описания бага, находит похожие существующие отчеты, " +
                    "использует LLM для определения дубликатов и улучшения исходного текста.")
    public CompletableFuture<BugAnalysisResponse> analyzeBugReport(@Valid @RequestBody BugAnalysisRequest request) {
        return bugAnalysisService.analyzeBugReport(request.draftDescription());
    }

    /**
     * Принимает "сырой" текст баг-репорта, структурирует его и проверяет на дубликаты.
     *
     * @param request DTO с текстом отчета от пользователя.
     * @return {@link CompletableFuture} с результатом, содержащим структурированный отчет и список дубликатов.
     */
    @PostMapping("/summarize-and-check")
    @Operation(summary = "Структурировать отчет и проверить на дубликаты",
            description = "Принимает неструктурированный текст, описывающий проблему, использует LLM для " +
                    "его структурирования, а затем по улучшенному тексту ищет дубликаты.")
    public CompletableFuture<List<AgentResult>> summarizeAndCheckBugReport(@Valid @RequestBody BugReportSummaryRequest request) {
        AgentContext context = request.toAgentContext();
        return orchestratorService.invokePipeline("bug-report-analysis-pipeline", context);
    }

    /**
     * Принимает "сырой" текст бага и генерирует для него исполняемый API-тест для воспроизведения.
     *
     * @param request DTO с текстом отчета от пользователя.
     * @return {@link CompletableFuture} с результатом, содержащим сгенерированный Java-код.
     */
    @PostMapping("/generate-repro-script")
    @Operation(summary = "Сгенерировать скрипт для воспроизведения бага",
            description = "Принимает неструктурированный текст, описывающий проблему, структурирует его " +
                    "и генерирует Java/RestAssured тест, который воспроизводит ошибку.")
    public CompletableFuture<List<AgentResult>> generateReproScript(@Valid @RequestBody BugReportSummaryRequest request) {
        AgentContext context = request.toAgentContext();
        return orchestratorService.invokePipeline("bug-reproduction-pipeline", context);
    }
}
