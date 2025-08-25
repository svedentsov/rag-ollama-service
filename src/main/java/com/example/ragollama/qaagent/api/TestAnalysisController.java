package com.example.ragollama.qaagent.api;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentOrchestratorService;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.api.dto.FlakyTestAnalysisRequest;
import com.example.ragollama.qaagent.api.dto.RootCauseAnalysisRequest;
import com.example.ragollama.qaagent.api.dto.TestCaseGenerationRequest;
import com.example.ragollama.qaagent.api.dto.TestVerificationRequest;
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
}
