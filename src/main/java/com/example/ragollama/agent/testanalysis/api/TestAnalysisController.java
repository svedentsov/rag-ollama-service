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
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Контроллер для управления AI-агентами, анализирующими тесты и требования.
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
     * @param request DTO с отчетами для сравнения.
     * @return {@link Mono} с результатом анализа.
     */
    @PostMapping("/detect-flaky")
    @Operation(summary = "Обнаружить 'плавающие' тесты")
    public Mono<List<AgentResult>> detectFlakyTests(@Valid @RequestBody FlakyTestAnalysisRequest request) {
        AgentContext context = request.toAgentContext();
        return orchestratorService.invoke("flaky-test-detection-pipeline", context);
    }

    /**
     * Запускает полный конвейер для анализа первопричины падения тестов.
     *
     * @param request DTO, содержащее все данные для анализа.
     * @return {@link Mono} с результатом, включающим вердикт RCA-агента.
     */
    @PostMapping("/analyze-root-cause")
    @Operation(summary = "Проанализировать первопричину падения тестов (RCA)")
    public Mono<List<AgentResult>> analyzeRootCause(@Valid @RequestBody RootCauseAnalysisRequest request) {
        return orchestratorService.invoke("root-cause-analysis-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для генерации тест-кейсов на основе требований.
     *
     * @param request DTO с текстовым описанием требований.
     * @return {@link Mono} с результатом, содержащим список сгенерированных тест-кейсов.
     */
    @PostMapping("/generate-test-cases")
    @Operation(summary = "Сгенерировать тест-кейсы из требований")
    public Mono<List<AgentResult>> generateTestCases(@Valid @RequestBody TestCaseGenerationRequest request) {
        return orchestratorService.invoke("test-case-generation-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для генерации чек-листа для ручного тестирования.
     *
     * @param request DTO с текстовым описанием функциональности.
     * @return {@link Mono} с результатом, содержащим сгенерированный чек-лист.
     */
    @PostMapping("/generate-checklist")
    @Operation(summary = "Сгенерировать чек-лист для ручного тестирования из описания")
    public Mono<List<AgentResult>> generateChecklist(@Valid @RequestBody ChecklistGenerationRequest request) {
        return orchestratorService.invoke("checklist-generation-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для проверки качества и полноты существующего теста.
     *
     * @param request DTO с кодом теста.
     * @return {@link Mono} с результатом, содержащим оценку теста.
     */
    @PostMapping("/verify")
    @Operation(summary = "Проверить качество существующего автотеста")
    public Mono<List<AgentResult>> verifyTestCode(@Valid @RequestBody TestVerificationRequest request) {
        return orchestratorService.invoke("test-verifier-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для анализа и рефакторинга кода автотеста.
     *
     * @param request DTO с кодом теста.
     * @return {@link Mono} с результатом, содержащим предложения по улучшению.
     */
    @PostMapping("/refactor")
    @Operation(summary = "Проанализировать и предложить рефакторинг для автотеста")
    public Mono<List<AgentResult>> refactorTestCode(@Valid @RequestBody TestRefactoringRequest request) {
        return orchestratorService.invoke("test-smell-refactoring-pipeline", request.toAgentContext());
    }

    /**
     * Запускает конвейер парного тестирования для генерации полного набора тестов.
     *
     * @param request DTO с описанием требований.
     * @return {@link Mono} с результатом, содержащим позитивный и негативные тесты.
     */
    @PostMapping("/pair-test")
    @Operation(summary = "Сгенерировать набор тестов с помощью двух AI-агентов (self-play)")
    public Mono<List<AgentResult>> runPairTesting(@Valid @RequestBody PairTestingRequest request) {
        return orchestratorService.invoke("agentic-pair-testing-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента "Тестовый Оракул" для анализа и объяснения набора тестов.
     *
     * @param request DTO с требованиями и сгенерированными тестами.
     * @return {@link Mono} с результатом, содержащим матрицу трассируемости.
     */
    @PostMapping("/explain-and-analyze-coverage")
    @Operation(summary = "Проанализировать тестовое покрытие и объяснить сгенерированные тесты (XAI)")
    public Mono<List<AgentResult>> analyzeTestCoverage(@Valid @RequestBody TestOracleRequest request) {
        return orchestratorService.invoke("xai-test-oracle-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента-наставника для проведения детального ревью автотеста.
     *
     * @param request DTO с требованиями и кодом теста.
     * @return {@link Mono} с результатом, содержащим отчет-ревью.
     */
    @PostMapping("/mentor-review")
    @Operation(summary = "Получить менторское ревью для автотеста (XAI)")
    public Mono<List<AgentResult>> getMentorshipReview(@Valid @RequestBody TestMentorshipRequest request) {
        return orchestratorService.invoke("test-mentor-pipeline", request.toAgentContext());
    }

    /**
     * Запускает конвейер для построения комплексного, иерархического чек-листа.
     *
     * @param request DTO с целью и Git-ссылками.
     * @return {@link Mono} с результатом, содержащим чек-лист.
     */
    @PostMapping("/build-checklist")
    @Operation(summary = "Построить комплексный, иерархический чек-лист")
    public Mono<List<AgentResult>> buildChecklist(@Valid @RequestBody ChecklistBuilderRequest request) {
        return orchestratorService.invoke("checklist-building-pipeline", request.toAgentContext());
    }

    /**
     * Запускает конвейер помощи мануальному QA для создания баг-репорта.
     *
     * @param request DTO с сырым описанием бага.
     * @return {@link Mono} с результатом, содержащим обогащенный отчет.
     */
    @PostMapping("/assist-bug-reporting")
    @Operation(summary = "Получить помощь в создании баг-репорта")
    public Mono<List<AgentResult>> assistBugReporting(@Valid @RequestBody BugReportAssistanceRequest request) {
        return orchestratorService.invoke("bug-reporting-assistance-pipeline", request.toAgentContext());
    }
}
