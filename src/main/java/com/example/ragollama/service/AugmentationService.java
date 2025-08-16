package com.example.ragollama.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Сервис, отвечающий исключительно за этап обогащения (Augmentation) в RAG-конвейере.
 * Его задача - принять список релевантных документов и исходный вопрос пользователя,
 * собрать из них единый контекст, соблюдая лимиты по токенам, и сформировать
 * финальный объект {@link Prompt}, готовый для отправки в LLM.
 */
@Service
@Slf4j
public class AugmentationService {

    private final ContextAssemblerService contextAssemblerService;
    private final PromptService promptService;

    /**
     * Конструктор для внедрения зависимостей.
     *
     * @param contextAssemblerService Сервис для интеллектуальной сборки контекста.
     * @param promptService           Сервис для создания промптов из шаблонов.
     */
    public AugmentationService(ContextAssemblerService contextAssemblerService, PromptService promptService) {
        this.contextAssemblerService = contextAssemblerService;
        this.promptService = promptService;
    }

    /**
     * Создает финальный промпт для LLM.
     *
     * @param documents Список релевантных документов, найденных на этапе Retrieval.
     * @param query     Оригинальный вопрос пользователя.
     * @return Готовый к отправке в LLM объект {@link Prompt}.
     */
    public Prompt augment(List<Document> documents, String query) {
        String context = contextAssemblerService.assembleContext(documents);

        if (context.isEmpty()) {
            log.warn("Контекст для запроса '{}' пуст. LLM будет отвечать без дополнительной информации.", query);
        }

        String promptString = promptService.createRagPrompt(Map.of("context", context, "question", query));
        return new Prompt(promptString);
    }
}
