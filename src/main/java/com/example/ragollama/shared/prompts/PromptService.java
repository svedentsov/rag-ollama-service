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
 * Сервис для ручного управления и рендеринга шаблонов FreeMarker.
 * <p>
 * Этот сервис является нашей собственной реализацией рендерера,
 * обходя ограничения стандартного PromptTemplate в Spring AI. Он загружает
 * все шаблоны при старте и предоставляет единый метод для их обработки.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final freemarker.template.Configuration freemarkerConfig;
    private final Map<String, Template> templateCache = new ConcurrentHashMap<>();

    /**
     * Централизованная карта всех используемых в приложении промптов.
     * Ключ - логическое имя, значение - путь к файлу.
     */
    private static final Map<String, String> TEMPLATE_PATHS = Map.ofEntries(
            Map.entry("ragPrompt", "rag-prompt.ftl"),
            Map.entry("queryTransformation", "query-transformation-prompt.ftl"),
            Map.entry("multiQuery", "multi-query-prompt.ftl"),
            Map.entry("codeGeneration", "code-generation-prompt.ftl"),
            Map.entry("routerAgent", "router-agent-prompt.ftl"),
            Map.entry("bugAnalysis", "bug-analysis-prompt.ftl"),
            Map.entry("grounding", "grounding-prompt.ftl"),
            Map.entry("summarization", "summarization-prompt.ftl"),
            Map.entry("releaseNotesWriter", "release-notes-writer-prompt.ftl"),
            Map.entry("bugReportSummarizer", "bug-report-summarizer-prompt.ftl"),
            Map.entry("impactAnalysis", "impact-analysis-prompt.ftl"),
            Map.entry("rbacExtractor", "rbac-extractor-prompt.ftl"),
            Map.entry("authRiskDetector", "auth-risk-detector-prompt.ftl"),
            Map.entry("testCaseGeneration", "test-case-generation-prompt.ftl"),
            Map.entry("specToTest", "spec-to-test-prompt.ftl"),
            Map.entry("authTestBuilder", "auth-test-builder-prompt.ftl"),
            Map.entry("rootCauseAnalysis", "root-cause-analysis-prompt.ftl"),
            Map.entry("syntheticDataBuilder", "synthetic-data-builder-prompt.ftl"),
            Map.entry("sourceCiteVerifier", "source-cite-verifier-prompt.ftl"),
            Map.entry("planningAgent", "planning-agent-prompt.ftl"),
            Map.entry("copilotResultSummarizer", "copilot-result-summarizer-prompt.ftl")
    );

    /**
     * Загружает и кэширует все известные шаблоны FreeMarker при старте приложения.
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
