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
    private static final Map<String, String> TEMPLATE_PATHS = Map.of(
            "ragPrompt", "rag-prompt.ftl",
            "queryTransformation", "query-transformation-prompt.ftl",
            "multiQuery", "multi-query-prompt.ftl",
            "codeGeneration", "code-generation-prompt.ftl",
            "routerAgent", "router-agent-prompt.ftl",
            "bugAnalysis", "bug-analysis-prompt.ftl",
            "grounding", "grounding-prompt.ftl",
            "summarization", "summarization-prompt.ftl"
    );

    /**
     * Загружает и кэширует все известные шаблоны FreeMarker при старте приложения.
     */
    @PostConstruct
    public void init() {
        TEMPLATE_PATHS.forEach((name, path) -> {
            try {
                Template template = freemarkerConfig.getTemplate(path);
                templateCache.put(name, template);
                log.info("Шаблон FreeMarker '{}' успешно загружен из '{}'", name, path);
            } catch (IOException e) {
                log.error("Не удалось загрузить шаблон '{}' из '{}'", name, path, e);
                throw new IllegalStateException("Ошибка инициализации PromptService", e);
            }
        });
    }

    /**
     * Обрабатывает (рендерит) именованный шаблон с предоставленной моделью данных.
     *
     * @param templateName Имя шаблона (ключ из карты TEMPLATE_PATHS).
     * @param model        Карта с данными для подстановки в шаблон.
     * @return Готовая строка промпта.
     * @throws IllegalStateException если шаблон не найден или произошла ошибка рендеринга.
     */
    public String render(String templateName, Map<String, Object> model) {
        Template template = templateCache.get(templateName);
        if (template == null) {
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
