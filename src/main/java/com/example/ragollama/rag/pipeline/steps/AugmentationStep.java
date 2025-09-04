package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.rag.advisors.RagAdvisor;
import com.example.ragollama.rag.domain.ContextAssemblerService;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Шаг RAG-конвейера, отвечающий за этап обогащения (Augmentation).
 * <p>
 * Его задача - принять извлеченные и переранжированные документы,
 * применить к ним бизнес-логику через цепочку "Советников" (Advisors),
 * а затем сформировать финальный, готовый к отправке в LLM промпт.
 */
@Component
@Order(30) // Выполняется после Reranking
@Slf4j
@RequiredArgsConstructor
public class AugmentationStep implements RagPipelineStep {

    private final List<RagAdvisor> advisors;
    private final ContextAssemblerService contextAssemblerService;
    private final PromptService promptService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [30] Augmentation: запуск обогащения контекста...");

        // Последовательно применяем всех советников к контексту
        Mono<RagFlowContext> finalContextMono = Flux.fromIterable(advisors)
                .reduce(Mono.just(context), (contextMono, advisor) -> contextMono.flatMap(advisor::advise))
                .flatMap(mono -> mono);

        return finalContextMono.map(finalContext -> {
            // Используем документы после реранжирования
            List<Document> documentsForContext = contextAssemblerService.assembleContext(finalContext.rerankedDocuments());
            String structuredContext = formatContextForCitation(documentsForContext);

            finalContext.promptModel().put("structuredContext", structuredContext);
            finalContext.promptModel().put("question", finalContext.originalQuery());
            finalContext.promptModel().put("history", formatHistory(finalContext.history()));

            String promptString = promptService.render("ragPrompt", finalContext.promptModel());
            log.debug("Этап Augmentation успешно завершен. Использовано {} документов в контексте.", documentsForContext.size());

            return finalContext.withFinalPrompt(new Prompt(promptString));
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
