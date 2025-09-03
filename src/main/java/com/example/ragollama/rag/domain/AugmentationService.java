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
 *
 * <p>Его задача - принять извлеченные документы и исходный запрос,
 * применить к ним различные бизнес-правила и эвристики через цепочку
 * "Советников" (Advisors), а затем, используя результат, сформировать
 * финальный, готовый к отправке в LLM промпт со структурированным контекстом
 * для поддержки inline-цитирования.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AugmentationService {

    private final List<RagAdvisor> advisors;
    private final ContextAssemblerService contextAssemblerService;
    private final PromptService promptService;

    /**
     * Асинхронно выполняет полный цикл обогащения контекста и генерации промпта.
     *
     * @param documents   Список документов, извлеченных на этапе Retrieval.
     * @param query       Оригинальный запрос пользователя.
     * @param chatHistory История предыдущего диалога для поддержания контекста.
     * @return {@link Mono}, который по завершении будет содержать полностью
     * сформированный и готовый к отправке в LLM объект {@link Prompt}.
     */
    public Mono<Prompt> augment(List<Document> documents, String query, List<Message> chatHistory) {
        RagContext initialContext = new RagContext(query);
        initialContext.setDocuments(documents);

        Mono<RagContext> finalContextMono = Flux.fromIterable(advisors)
                .reduce(Mono.just(initialContext), (contextMono, advisor) -> contextMono.flatMap(advisor::advise))
                .flatMap(mono -> mono);

        return finalContextMono.map(finalContext -> {
            List<Document> documentsForContext = contextAssemblerService.assembleContext(finalContext.getDocuments());
            String structuredContext = formatContextForCitation(documentsForContext);

            finalContext.getPromptModel().put("structuredContext", structuredContext);
            finalContext.getPromptModel().put("question", query);
            String historyString = formatHistory(chatHistory);
            finalContext.getPromptModel().put("history", historyString);

            String promptString = promptService.render("ragPrompt", finalContext.getPromptModel());
            log.debug("Этап Augmentation успешно завершен. Использовано {} документов в контексте.", documentsForContext.size());
            return new Prompt(promptString);
        });
    }

    /**
     * Форматирует список документов в XML-подобную структуру для LLM.
     *
     * @param documents Список документов для форматирования.
     * @return Строка со структурированным контекстом.
     */
    private String formatContextForCitation(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "<no_documents_found />";
        }
        return documents.stream()
                .map(doc -> String.format("<document id=\"%s\" source=\"%s\">\n%s\n</document>",
                        doc.getMetadata().get("chunkId"),
                        doc.getMetadata().get("source"),
                        doc.getText()))
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Форматирует историю сообщений в единую строку для подстановки в промпт.
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
