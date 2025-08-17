package com.example.ragollama.service;

import com.example.ragollama.rag.RagAdvisor;
import com.example.ragollama.rag.RagContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис, отвечающий за этап обогащения (Augmentation) в RAG-конвейере.
 * Эта версия полностью переработана для использования паттерна "Советник" (Advisor).
 * Вместо жестко закодированной логики, сервис теперь делегирует все операции
 * по модификации контекста и промпта цепочке бинов, реализующих интерфейс
 * {@link RagAdvisor}. Это делает систему гибкой, расширяемой и легко тестируемой.
 */
@Service
@Slf4j
public class AugmentationService {

    private final List<RagAdvisor> advisors;
    private final ContextAssemblerService contextAssemblerService;
    private final PromptService promptService;

    /**
     * Конструктор для внедрения зависимостей.
     * Spring автоматически соберет все бины, реализующие интерфейс {@link RagAdvisor},
     * и внедрит их в виде списка. Порядок выполнения можно контролировать
     * с помощью аннотации {@code @Order} на классах-советниках.
     *
     * @param advisors                Список всех активных RAG-советников в приложении.
     * @param contextAssemblerService Сервис для интеллектуальной сборки контекста из документов.
     * @param promptService           Сервис для создания промптов из шаблонов.
     */
    public AugmentationService(List<RagAdvisor> advisors, ContextAssemblerService contextAssemblerService, PromptService promptService) {
        this.advisors = advisors;
        this.contextAssemblerService = contextAssemblerService;
        this.promptService = promptService;
        log.info("AugmentationService инициализирован с {} советниками.", advisors.size());
    }

    /**
     * Создает финальный промпт для LLM, прогоняя контекст через цепочку советников.
     *
     * @param documents Список релевантных документов, найденных на предыдущих этапах.
     * @param query     Оригинальный вопрос пользователя.
     * @return Готовый к отправке в LLM объект {@link Prompt}.
     */
    public Prompt augment(List<Document> documents, String query) {
        // 1. Создаем начальный контекстный объект.
        RagContext context = new RagContext(query);
        context.setDocuments(documents);
        // 2. Последовательно применяем всех советников. Каждый может изменить контекст.
        log.debug("Начало выполнения цепочки из {} советников для запроса: '{}'", advisors.size(), query);
        for (RagAdvisor advisor : advisors) {
            context = advisor.advise(context);
        }
        log.debug("Цепочка советников успешно выполнена.");
        // 3. Собираем финальную строку контекста из документов (они могли быть изменены).
        String contextString = contextAssemblerService.assembleContext(context.getDocuments());
        context.getPromptModel().put("context", contextString);
        context.getPromptModel().put("question", query);
        // 4. Рендерим финальный промпт, используя модель, обогащенную советниками.
        String promptString = promptService.createRagPrompt(context.getPromptModel());
        return new Prompt(promptString);
    }
}
