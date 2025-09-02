package com.example.ragollama.shared.prompts;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для централизованного управления и рендеринга шаблонов FreeMarker.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final freemarker.template.Configuration freemarkerConfig;
    private final Map<String, Template> templateCache = new ConcurrentHashMap<>();

    /**
     * Централизованный каталог всех используемых в приложении промптов.
     */
    private static final Map<String, String> TEMPLATE_PATHS = Map.ofEntries(
            // RAG & Chat
            Map.entry("ragPrompt", "rag-prompt.ftl"),
            Map.entry("routerAgent", "router-agent-prompt.ftl"),

            // RAG Agents
            Map.entry("queryTransformation", "query-transformation-prompt.ftl"),
            Map.entry("multiQuery", "multi-query-prompt.ftl"),

            // Code Generation & Remediation
            Map.entry("codeGeneration", "code-generation-prompt.ftl"),
            Map.entry("codeRemediation", "code-remediation-prompt.ftl"),
            Map.entry("testGenerator", "test-generator-prompt.ftl"),
            Map.entry("selfImprovingTest", "self-improving-test-prompt.ftl"),
            Map.entry("contractTestGenerator", "contract-test-generator-prompt.ftl"),
            Map.entry("bugReproScriptGenerator", "bug-repro-script-generator-prompt.ftl"),
            Map.entry("e2eFlowSynthesizer", "e2e-flow-synthesizer-prompt.ftl"),
            Map.entry("dastTestGenerator", "dast-test-generator-prompt.ftl"),
            Map.entry("fuzzingTestGenerator", "fuzzing-test-generator-prompt.ftl"),

            // Analysis Agents
            Map.entry("bugAnalysis", "bug-analysis-prompt.ftl"),
            Map.entry("impactAnalysis", "impact-analysis-prompt.ftl"),
            Map.entry("rootCauseAnalysis", "root-cause-analysis-prompt.ftl"),
            Map.entry("testVerifier", "test-verifier-prompt.ftl"),
            Map.entry("codeQualityImpact", "code-quality-impact-prompt.ftl"),
            Map.entry("defectEconomics", "defect-economics-prompt.ftl"),
            Map.entry("securityRiskScorer", "security-risk-scorer-prompt.ftl"),
            Map.entry("testCaseDeduplication", "test-case-deduplication-prompt.ftl"),
            Map.entry("accessibilityAudit", "accessibility-audit-prompt.ftl"),
            Map.entry("testSmellRefactorer", "test-smell-refactorer-prompt.ftl"),
            Map.entry("canaryAnalysis", "canary-analysis-prompt.ftl"),
            Map.entry("sastAgent", "sast-agent-prompt.ftl"),
            Map.entry("securityLogAnalyzer", "security-log-analyzer-prompt.ftl"),
            Map.entry("securityReportAggregator", "security-report-aggregator-prompt.ftl"),
            Map.entry("personaGenerator", "persona-generator-prompt.ftl"),
            Map.entry("complianceReportGenerator", "compliance-report-generator-prompt.ftl"),
            Map.entry("archConsistencyMapper", "arch-consistency-mapper-prompt.ftl"),
            Map.entry("scaComplianceAgent", "sca-compliance-agent-prompt.ftl"),
            Map.entry("mlDriftGuard", "ml-drift-guard-prompt.ftl"),
            Map.entry("xaiTestOracle", "xai-test-oracle-prompt.ftl"),
            Map.entry("testDesigner", "test-designer-prompt.ftl"),
            Map.entry("adversarialTester", "adversarial-tester-prompt.ftl"),
            Map.entry("uxHeuristicsEvaluator", "ux-heuristics-evaluator-prompt.ftl"),
            Map.entry("userBehaviorSimulator", "user-behavior-simulator-prompt.ftl"),
            Map.entry("privacyComplianceChecker", "privacy-compliance-checker-prompt.ftl"),
            Map.entry("testMentorBot", "test-mentor-bot-prompt.ftl"),
            Map.entry("cypherQueryGenerator", "cypher-query-generator-prompt.ftl"),
            Map.entry("feedbackToTest", "feedback-to-test-prompt.ftl"),
            Map.entry("canaryDecisionMaker", "canary-decision-maker-prompt.ftl"),
            Map.entry("checklistBuilder", "checklist-builder-prompt.ftl"),
            Map.entry("bugPatternDetector", "bug-pattern-detector-prompt.ftl"),
            Map.entry("refactoringStrategist", "refactoring-strategist-prompt.ftl"),
            Map.entry("sprintPlanner", "sprint-planner-prompt.ftl"),
            Map.entry("architecturalReviewSynthesizer", "architectural-review-synthesizer-prompt.ftl"),
            Map.entry("incidentSummarizer", "incident-summarizer-prompt.ftl"),
            Map.entry("featureExtraction", "feature-extraction-prompt.ftl"),
            Map.entry("featureGapAnalysis", "feature-gap-analysis-prompt.ftl"),
            Map.entry("documentEnhancer", "document-enhancer-prompt.ftl"),
            Map.entry("resourceAllocator", "resource-allocator-prompt.ftl"),
            Map.entry("prioritizationAgent", "prioritization-agent-prompt.ftl"),
            Map.entry("federatedInsights", "federated-insights-prompt.ftl"),
            Map.entry("knowledgeRouter", "knowledge-router-prompt.ftl"),
            Map.entry("strategicInitiativePlanner", "strategic-initiative-planner-prompt.ftl"),

            // Summarization & Data Generation
            Map.entry("summarization", "summarization-prompt.ftl"),
            Map.entry("bugReportSummarizer", "bug-report-summarizer-prompt.ftl"),
            Map.entry("releaseNotesWriter", "release-notes-writer-prompt.ftl"),
            Map.entry("syntheticDataBuilder", "synthetic-data-builder-prompt.ftl"),
            Map.entry("dataSubsetSqlGenerator", "data-subset-sql-generator-prompt.ftl"),
            Map.entry("dpSyntheticDataGenerator", "dp-synthetic-data-generator-prompt.ftl"),
            Map.entry("dataGenerator", "data-generator-prompt.ftl"),
            Map.entry("testCaseGeneration", "test-case-generation-prompt.ftl"),
            Map.entry("checklistGenerator", "checklist-generator-prompt.ftl"),
            Map.entry("dataSummarizer", "data-summarizer-prompt.ftl"),
            Map.entry("chartGenerator", "chart-generator-prompt.ftl"),

            // Validation & Explanation
            Map.entry("grounding", "grounding-prompt.ftl"),
            Map.entry("sourceCiteVerifier", "source-cite-verifier-prompt.ftl"),
            Map.entry("explainerAgent", "explainer-agent-prompt.ftl"),
            Map.entry("crossValidator", "cross-validator-prompt.ftl"),
            Map.entry("trustScorer", "trust-scorer-prompt.ftl"),

            // Strategic & Planning Agents
            Map.entry("planningAgent", "planning-agent-prompt.ftl"),
            Map.entry("autonomousTriage", "autonomous-triage-prompt.ftl"),
            Map.entry("qaLeadStrategy", "qa-lead-strategy-prompt.ftl"),
            Map.entry("sdlcStrategy", "sdlc-strategy-prompt.ftl"),
            Map.entry("workflowPlanner", "workflow-planner-prompt.ftl"),
            Map.entry("testTrendAnalyzer", "test-trend-analyzer-prompt.ftl"),
            Map.entry("regressionPredictor", "regression-predictor-prompt.ftl"),
            Map.entry("testDebtSummary", "test-debt-summary-prompt.ftl"),
            Map.entry("riskMatrixSummary", "risk-matrix-summary-prompt.ftl"),
            Map.entry("prReviewAggregator", "pr-review-aggregator-prompt.ftl"),
            Map.entry("copilotResultSummarizer", "copilot-result-summarizer-prompt.ftl"),
            Map.entry("releaseDecision", "release-decision-prompt.ftl"),
            Map.entry("releaseReadiness", "release-readiness-prompt.ftl"),
            Map.entry("customerImpactAnalyzer", "customer-impact-analyzer-prompt.ftl"),

            // Optimization & Meta-Learning
            Map.entry("ragOptimizer", "rag-optimizer-prompt.ftl"),
            Map.entry("queryProfiler", "query-profiler-prompt.ftl"),
            Map.entry("errorHandler", "error-handler-prompt.ftl"),
            Map.entry("promptRefinement", "prompt-refinement-prompt.ftl"),
            Map.entry("simulationAnalyzer", "simulation-analyzer-prompt.ftl"),
            Map.entry("experimentAnalyzer", "experiment-analyzer-prompt.ftl")
    );

    /**
     * Загружает и кэширует все известные шаблоны FreeMarker при старте приложения.
     * <p>
     * Этот метод вызывается автоматически после создания бина благодаря аннотации
     * {@code @PostConstruct}. Он итерируется по статической карте {@code TEMPLATE_PATHS},
     * загружает каждый шаблон и помещает его в потокобезопасный кэш для
     * быстрого доступа в дальнейшем.
     *
     * @throws IllegalStateException если какой-либо из шаблонов не может быть загружен.
     */
    @PostConstruct
    public void init() {
        log.info("Начало кэширования {} шаблонов FreeMarker.", TEMPLATE_PATHS.size());
        TEMPLATE_PATHS.forEach((name, path) -> {
            try {
                Template template = freemarkerConfig.getTemplate(path);
                templateCache.put(name, template);
                log.debug("Шаблон FreeMarker '{}' успешно загружен из '{}'", name, path);
            } catch (IOException e) {
                log.error("Критическая ошибка: не удалось загрузить шаблон '{}' из '{}'", name, path, e);
                throw new IllegalStateException("Ошибка инициализации PromptService", e);
            }
        });
        log.info("Кэширование шаблонов FreeMarker успешно завершено.");
    }

    /**
     * Обрабатывает (рендерит) именованный шаблон с предоставленной моделью данных.
     *
     * @param templateName Имя шаблона (ключ из карты {@code TEMPLATE_PATHS}).
     * @param model        Карта с данными для подстановки в шаблон.
     * @return Готовая строка промпта, готовая к передаче в LLM.
     * @throws IllegalStateException если шаблон с указанным именем не был найден
     *                               в кэше или произошла ошибка в процессе рендеринга.
     */
    public String render(String templateName, Map<String, Object> model) {
        Template template = templateCache.get(templateName);
        if (template == null) {
            log.error("Попытка использования незарегистрированного шаблона: '{}'", templateName);
            throw new IllegalStateException("Шаблон с именем '" + templateName + "' не найден в кэше.");
        }

        try (StringWriter writer = new StringWriter()) {
            template.process(model, writer);
            return writer.toString();
        } catch (TemplateException | IOException e) {
            log.error("Ошибка при обработке шаблона FreeMarker '{}'", templateName, e);
            throw new IllegalStateException("Ошибка рендеринга промпта '" + templateName + "'", e);
        }
    }
}
