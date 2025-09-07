package com.example.ragollama.shared.prompts;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для централизованного управления и рендеринга шаблонов FreeMarker.
 * *
 * <p>Эта версия динамически сканирует директорию `resources/prompts` при старте
 * приложения, автоматически обнаруживая и кэшируя все доступные шаблоны.
 * Это устраняет необходимость в ручной регистрации каждого промпта и делает
 * систему более гибкой и поддерживаемой.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final freemarker.template.Configuration freemarkerConfig;
    private final Map<String, Template> templateCache = new ConcurrentHashMap<>();

    /**
     * Динамически сканирует, загружает и кэширует все шаблоны FreeMarker из
     * директории `resources/prompts` при старте приложения.
     *
     * @throws IllegalStateException если произошла ошибка при сканировании или
     *                               загрузке шаблонов, что является критической
     *                               ошибкой для работы приложения.
     */
    @PostConstruct
    public void init() {
        log.info("Начало динамического сканирования и кэширования шаблонов FreeMarker...");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath:/prompts/**/*.ftl");
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName != null) {
                    String templateName = toCamelCase(fileName.replace(".ftl", ""));
                    Template template = freemarkerConfig.getTemplate(fileName);
                    templateCache.put(templateName, template);
                    log.trace("Шаблон '{}' из файла '{}' успешно загружен и закэширован.", templateName, fileName);
                }
            }
        } catch (IOException e) {
            log.error("Критическая ошибка: не удалось просканировать директорию с шаблонами.", e);
            throw new IllegalStateException("Ошибка инициализации PromptService", e);
        }
        log.info("Кэширование {} шаблонов FreeMarker успешно завершено.", templateCache.size());
    }

    /**
     * Обрабатывает (рендерит) именованный шаблон с предоставленной моделью данных.
     *
     * @param templateName Имя шаблона в формате camelCase (например, "ragPrompt").
     * @param model        Карта с данными для подстановки в шаблон.
     * @return Готовая строка промпта, готовая к передаче в LLM.
     * @throws IllegalStateException если шаблон с указанным именем не был найден
     *                               в кэше или произошла ошибка в процессе рендеринга.
     */
    public String render(String templateName, Map<String, Object> model) {
        Template template = templateCache.get(templateName);
        if (template == null) {
            log.error("Попытка использования незарегистрированного шаблона: '{}'. Доступные шаблоны: {}",
                    templateName, templateCache.keySet());
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

    /**
     * Преобразует имя файла в стиле kebab-case в camelCase.
     *
     * @param s Имя файла без расширения.
     * @return Имя в формате camelCase.
     */
    private String toCamelCase(String s) {
        String[] parts = s.split("-");
        StringBuilder camelCaseString = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            camelCaseString.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1));
        }
        return camelCaseString.toString();
    }
}
