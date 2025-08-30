package com.example.ragollama.agent.testanalysis.api;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.buganalysis.api.dto.BugReportAssistanceRequest;
import com.example.ragollama.agent.testanalysis.api.dto.*;
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
 * Контроллер для управления AI-агентами, анализирующими тесты и требования.
 * <p>
 * Предоставляет API для выполнения различных задач, связанных с качеством
 * программного обеспечения, таких как обнаружение "плавающих" тестов,
 * анализ первопричин, генерация тест-кейсов и проверка качества кода тестов.
 */
@RestController
@RequestMapping("/api/v1/agents/tests")
@RequiredArgsConstructor
@Tag(name = "Test Analysis & Generation Agents", description = "API для анализа и генерации артефактов тестирования")
public class TestAnalysisController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает агента для обнаружения "плавающих" тестов.
     *
     * @param request DTO с содержимым двух JUnit XML отчетов для сравнения.
     * @return {@link CompletableFuture} с результатом анализа, содержащим
     * список потенциально "плавающих" тестов.
     */
    @PostMapping("/detect-flaky")
    @Operation(summary = "Обнаружить 'плавающие' тесты",
            description = "Сравнивает отчет о тестировании с текущей ветки с отчетом с основной ветки " +
                    "для выявления тестов, которые упали сейчас, но проходили ранее.")
    public CompletableFuture<List<AgentResult>> detectFlakyTests(@Valid @RequestBody FlakyTestAnalysisRequest request) {
        AgentContext context = request.toAgentContext();
        return orchestratorService.invokePipeline("flaky-test-detection-pipeline", context);
    }

    /**
     * Запускает полный конвейер для анализа первопричины падения тестов.
     * <p>
     * Этот эндпоинт принимает все необходимые "улики" (отчеты о тестах,
     * Git-ссылки для получения diff и логи приложения), запускает
     * многошаговый конвейер и возвращает структурированную гипотезу
     * о первопричине сбоя.
     *
     * @param request DTO, содержащее все данные для анализа.
     * @return {@link CompletableFuture} с результатом, включающим вердикт RCA-агента.
     */
    @PostMapping("/analyze-root-cause")
    @Operation(summary = "Проанализировать первопричину падения тестов (RCA)",
            description = "Запускает 'root-cause-analysis-pipeline', который находит 'плавающие' тесты, " +
                    "измененный код и анализирует их вместе с логами для поиска первопричины.")
    public CompletableFuture<List<AgentResult>> analyzeRootCause(@Valid @RequestBody RootCauseAnalysisRequest request) {
        return orchestratorService.invokePipeline("root-cause-analysis-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для генерации тест-кейсов на основе требований.
     *
     * @param request DTO с текстовым описанием требований.
     * @return {@link CompletableFuture} с результатом, содержащим список сгенерированных тест-кейсов.
     */
    @PostMapping("/generate-test-cases")
    @Operation(summary = "Сгенерировать тест-кейсы из требований")
    public CompletableFuture<List<AgentResult>> generateTestCases(@Valid @RequestBody TestCaseGenerationRequest request) {
        return orchestratorService.invokePipeline("test-case-generation-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для генерации чек-листа для ручного тестирования.
     *
     * @param request DTO с текстовым описанием функциональности.
     * @return {@link CompletableFuture} с результатом, содержащим сгенерированный чек-лист.
     */
    @PostMapping("/generate-checklist")
    @Operation(summary = "Сгенерировать чек-лист для ручного тестирования из описания")
    public CompletableFuture<List<AgentResult>> generateChecklist(@Valid @RequestBody ChecklistGenerationRequest request) {
        return orchestratorService.invokePipeline("checklist-generation-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для проверки качества и полноты существующего теста.
     *
     * @param request DTO, содержащее исходный код теста для анализа.
     * @return {@link CompletableFuture} с результатом, содержащим структурированную оценку теста.
     */
    @PostMapping("/verify")
    @Operation(summary = "Проверить качество существующего автотеста",
            description = "Принимает исходный код Java-теста и использует LLM для его анализа на соответствие лучшим практикам.")
    public CompletableFuture<List<AgentResult>> verifyTestCode(@Valid @RequestBody TestVerificationRequest request) {
        return orchestratorService.invokePipeline("test-verifier-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для анализа и рефакторинга кода автотеста.
     *
     * @param request DTO, содержащее исходный код теста для анализа.
     * @return {@link CompletableFuture} с результатом, содержащим предложения по улучшению.
     */
    @PostMapping("/refactor")
    @Operation(summary = "Проанализировать и предложить рефакторинг для автотеста",
            description = "Принимает исходный код Java-теста, находит в нем 'запахи' и генерирует улучшенную версию.")
    public CompletableFuture<List<AgentResult>> refactorTestCode(@Valid @RequestBody TestRefactoringRequest request) {
        return orchestratorService.invokePipeline("test-smell-refactoring-pipeline", request.toAgentContext());
    }

    /**
     * Запускает конвейер парного тестирования для генерации полного набора тестов.
     *
     * @param request DTO с описанием требований.
     * @return {@link CompletableFuture} с результатом, содержащим позитивный и негативные тесты.
     */
    @PostMapping("/pair-test")
    @Operation(summary = "Сгенерировать набор тестов с помощью двух AI-агентов (self-play)",
            description = "Один агент генерирует позитивный тест, второй анализирует его и генерирует негативные/граничные тесты для упущенных сценариев.")
    public CompletableFuture<List<AgentResult>> runPairTesting(@Valid @RequestBody PairTestingRequest request) {
        return orchestratorService.invokePipeline("agentic-pair-testing-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента "Тестовый Оракул" для анализа и объяснения набора тестов.
     *
     * @param request DTO с требованиями и сгенерированными тестами.
     * @return {@link CompletableFuture} с результатом, содержащим матрицу трассируемости и анализ покрытия.
     */
    @PostMapping("/explain-and-analyze-coverage")
    @Operation(summary = "Проанализировать тестовое покрытие и объяснить сгенерированные тесты (XAI)",
            description = "Принимает требования и набор тестов, строит матрицу трассируемости и находит непокрытые требования.")
    public CompletableFuture<List<AgentResult>> analyzeTestCoverage(@Valid @RequestBody TestOracleRequest request) {
        return orchestratorService.invokePipeline("xai-test-oracle-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента-наставника для проведения детального ревью автотеста.
     *
     * @param request DTO с требованиями и кодом теста.
     * @return {@link CompletableFuture} с результатом, содержащим полный отчет-ревью.
     */
    @PostMapping("/mentor-review")
    @Operation(summary = "Получить менторское ревью для автотеста (XAI)")
    public CompletableFuture<List<AgentResult>> getMentorshipReview(@Valid @RequestBody TestMentorshipRequest request) {
        return orchestratorService.invokePipeline("test-mentor-pipeline", request.toAgentContext());
    }

    /**
     * Запускает конвейер для построения комплексного, иерархического чек-листа.
     *
     * @param request DTO с целью и Git-ссылками для контекста.
     * @return {@link CompletableFuture} с результатом, содержащим чек-лист в формате Markdown.
     */
    @PostMapping("/build-checklist")
    @Operation(summary = "Построить комплексный, иерархический чек-лист")
    public CompletableFuture<List<AgentResult>> buildChecklist(@Valid @RequestBody ChecklistBuilderRequest request) {
        return orchestratorService.invokePipeline("checklist-building-pipeline", request.toAgentContext());
    }

    /**
     * Запускает конвейер помощи мануальному QA для создания баг-репорта.
     *
     * @param request DTO с сырым описанием бага и контекстом.
     * @return {@link CompletableFuture} с результатом, содержащим обогащенный отчет.
     */
    @PostMapping("/assist-bug-reporting")
    @Operation(summary = "Получить помощь в создании баг-репорта")
    public CompletableFuture<List<AgentResult>> assistBugReporting(@Valid @RequestBody BugReportAssistanceRequest request) {
        return orchestratorService.invokePipeline("bug-reporting-assistance-pipeline", request.toAgentContext());
    }
}
