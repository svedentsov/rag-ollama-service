package com.example.ragollama.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Сервис для управления и создания промптов из шаблонов.
 * <p>
 * Централизует логику загрузки и рендеринга шаблонов промптов,
 * что позволяет легко изменять их без перекомпиляции кода.
 */
@Service
@Slf4j
public class PromptService {

    private final ResourceLoader resourceLoader;
    private final String ragTemplatePath;
    private PromptTemplate ragPromptTemplate;

    /**
     * Конструктор сервиса.
     *
     * @param resourceLoader  Стандартный загрузчик ресурсов Spring.
     * @param ragTemplatePath Путь к файлу шаблона RAG-промпта, заданный в {@code application.yml}.
     */
    public PromptService(ResourceLoader resourceLoader, @Value("${app.prompt.rag-template-path}") String ragTemplatePath) {
        this.resourceLoader = resourceLoader;
        this.ragTemplatePath = ragTemplatePath;
    }

    /**
     * Инициализирует сервис после создания бина.
     * <p>
     * Загружает шаблон промпта из файла в classpath, компилирует его
     * и сохраняет в поле {@code ragPromptTemplate} для дальнейшего использования.
     * В случае ошибки загрузки выбрасывает исключение, останавливая запуск приложения.
     */
    @PostConstruct
    private void init() {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + ragTemplatePath);
            String templateString = resource.getContentAsString(StandardCharsets.UTF_8);
            this.ragPromptTemplate = new PromptTemplate(templateString);
            log.info("Шаблон RAG-промпта успешно загружен из: {}", ragTemplatePath);
        } catch (IOException e) {
            log.error("Не удалось загрузить шаблон RAG-промпта из: {}", ragTemplatePath, e);
            throw new IllegalStateException("Не удалось инициализировать PromptService", e);
        }
    }

    /**
     * Создает финальный текст промпта для RAG-запроса, подставляя данные в шаблон.
     *
     * @param model Карта (Map), содержащая переменные для подстановки в шаблон
     *              (например, "context" и "question").
     * @return Готовый к отправке в LLM текст промпта.
     */
    public String createRagPrompt(Map<String, Object> model) {
        return this.ragPromptTemplate.render(model);
    }
}
