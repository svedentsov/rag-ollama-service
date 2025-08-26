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
 * <p>
 * Этот сервис является нашей собственной реализацией рендерера,
 * обходя ограничения стандартного PromptTemplate в Spring AI. Он загружает
 * все шаблоны при старте приложения и предоставляет единый метод для их
 * безопасной и эффективной обработки.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final freemarker.template.Configuration freemarkerConfig;
    private final Map<String, Template> templateCache = new ConcurrentHashMap<>();

    /**
     * Централизованный каталог всех используемых в приложении промптов.
     * Ключ - логическое имя, используемое в коде. Значение - путь к файлу в ресурсах.
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

            // Analysis Agents
            Map.entry("bugAnalysis", "bug-analysis-prompt.ftl"),
            Map.entry("impactAnalysis", "impact-analysis-prompt.ftl"),
            Map.entry("rootCauseAnalysis", "root-cause-analysis-prompt.ftl"),
            Map.entry("testVerifier", "test-verifier-prompt.ftl"),
            Map.entry("codeQualityImpact", "code-quality-impact-prompt.ftl"),
            Map.entry("defectEconomics", "defect-economics-prompt.ftl"),
            Map.entry("securityRiskScorer", "security-risk-scorer-prompt.ftl"),

            // Summarization & Data Generation
            Map.entry("summarization", "summarization-prompt.ftl"),
            Map.entry("bugReportSummarizer", "bug-report-summarizer-prompt.ftl"),
            Map.entry("releaseNotesWriter", "release-notes-writer-prompt.ftl"),
            Map.entry("syntheticDataBuilder", "synthetic-data-builder-prompt.ftl"),
            Map.entry("testCaseGeneration", "test-case-generation-prompt.ftl"),

            // Validation & Explanation
            Map.entry("grounding", "grounding-prompt.ftl"),
            Map.entry("sourceCiteVerifier", "source-cite-verifier-prompt.ftl"),
            Map.entry("explainerAgent", "explainer-agent-prompt.ftl"),

            // Strategic & Planning Agents
            Map.entry("planningAgent", "planning-agent-prompt.ftl"),
            Map.entry("autonomousTriage", "autonomous-triage-prompt.ftl"),
            Map.entry("qaLeadStrategy", "qa-lead-strategy-prompt.ftl"),
            Map.entry("sdlcStrategy", "sdlc-strategy-prompt.ftl"),
            Map.entry("testTrendAnalyzer", "test-trend-analyzer-prompt.ftl"),
            Map.entry("regressionPredictor", "regression-predictor-prompt.ftl"),
            Map.entry("testDebtSummary", "test-debt-summary-prompt.ftl"),
            Map.entry("riskMatrixSummary", "risk-matrix-summary-prompt.ftl")
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
