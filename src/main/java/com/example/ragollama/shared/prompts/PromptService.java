package com.example.ragollama.shared.prompts;

import com.example.ragollama.shared.config.properties.AppProperties;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Сервис для управления и создания промптов из шаблонов.
 * <p>
 * Эта версия полностью переведена на использование движка шаблонов FreeMarker.
 * Это позволяет работать со сложными структурами данных (списками, объектами)
 * прямо в шаблоне, отделяя логику подготовки данных от их представления.
 * Spring Boot автоматически сконфигурирует бин {@link Configuration},
 * если в classpath есть зависимость `spring-boot-starter-freemarker`.
 */
@Slf4j
@Service
public class PromptService {

    private final String ragTemplatePath;
    private final Configuration freemarkerConfig;
    private Template ragPromptTemplate;

    /**
     * Конструктор сервиса.
     *
     * @param appProperties    Объект с настройками приложения.
     * @param freemarkerConfig Автоматически сконфигурированный бин FreeMarker.
     */
    public PromptService(AppProperties appProperties, Configuration freemarkerConfig) {
        this.ragTemplatePath = appProperties.prompt().ragTemplatePath();
        this.freemarkerConfig = freemarkerConfig;
    }

    /**
     * Инициализирует сервис, загружая и компилируя шаблон FreeMarker.
     * Вызывается один раз после создания бина.
     *
     * @throws IllegalStateException если шаблон не может быть загружен.
     */
    @PostConstruct
    private void init() {
        try {
            this.ragPromptTemplate = freemarkerConfig.getTemplate(ragTemplatePath);
            log.info("Шаблон RAG-промпта FreeMarker успешно загружен из: {}", ragTemplatePath);
        } catch (IOException e) {
            log.error("Не удалось загрузить шаблон RAG-промпта FreeMarker из: {}. Убедитесь, что путь в application.yml (spring.freemarker.template-loader-path) указан верно.", ragTemplatePath, e);
            throw new IllegalStateException("Не удалось инициализировать PromptService с FreeMarker", e);
        }
    }

    /**
     * Создает финальный текст промпта, обрабатывая шаблон FreeMarker с предоставленной моделью данных.
     *
     * @param model Карта (Map), содержащая переменные для подстановки в шаблон
     *              (например, "documents", "question").
     * @return Готовый к отправке в LLM текст промпта.
     * @throws IllegalStateException если происходит ошибка во время обработки шаблона.
     */
    public String createRagPrompt(Map<String, Object> model) {
        try (StringWriter writer = new StringWriter()) {
            ragPromptTemplate.process(model, writer);
            return writer.toString();
        } catch (TemplateException | IOException e) {
            log.error("Ошибка при обработке шаблона RAG-промпта FreeMarker", e);
            throw new IllegalStateException("Ошибка рендеринга RAG-промпта", e);
        }
    }
}
