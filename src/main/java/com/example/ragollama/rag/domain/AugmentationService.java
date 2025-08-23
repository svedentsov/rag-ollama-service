package com.example.ragollama.rag.domain;

import com.example.ragollama.rag.advisors.RagAdvisor;
import com.example.ragollama.rag.model.RagContext;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис, отвечающий за этап обогащения (Augmentation) в RAG-конвейере.
 * <p>
 * Эта версия использует наш кастомный {@link PromptService} для рендеринга
 * шаблона, что обеспечивает полный контроль и независимость от
 * стандартного рендерера Spring AI.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AugmentationService {

    private final List<RagAdvisor> advisors;
    private final ContextAssemblerService contextAssemblerService;
    private final PromptService promptService;

    /**
     * Выполняет обогащение, последовательно применяя все доступные советники
     * и формируя промпт с помощью кастомного {@link PromptService}.
     *
     * @param documents   Извлеченные документы.
     * @param query       Запрос пользователя.
     * @param chatHistory История чата.
     * @return {@link Mono} с полностью готовым промптом.
     */
    public Mono<Prompt> augment(List<Document> documents, String query, List<Message> chatHistory) {
        RagContext initialContext = new RagContext(query);
        initialContext.setDocuments(documents);

        Mono<RagContext> finalContextMono = Flux.fromIterable(advisors)
                .reduce(Mono.just(initialContext), (contextMono, advisor) -> contextMono.flatMap(advisor::advise))
                .flatMap(mono -> mono);

        return finalContextMono.map(finalContext -> {
            List<Document> documentsForContext = contextAssemblerService.assembleContext(finalContext.getDocuments());

            finalContext.getPromptModel().put("documents", documentsForContext);
            finalContext.getPromptModel().put("question", query);
            String historyString = formatHistory(chatHistory);
            finalContext.getPromptModel().put("history", historyString);
            String promptString = promptService.render("ragPrompt", finalContext.getPromptModel());
            log.debug("Этап Augmentation успешно завершен. Использовано {} документов в контексте.", documentsForContext.size());
            return new Prompt(promptString);
        });
    }

    /**
     * Форматирует историю сообщений в строку для подстановки в промпт.
     *
     * @param chatHistory Список сообщений из Spring AI.
     * @return Отформатированная строка истории.
     */
    private String formatHistory(List<Message> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return "Нет истории.";
        }
        return chatHistory.stream()
                .map(m -> m.getMessageType().getValue() + ": " + m.getText())
                .collect(Collectors.joining("\n"));
    }
}
