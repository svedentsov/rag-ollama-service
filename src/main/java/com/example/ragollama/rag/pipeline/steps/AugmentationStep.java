package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.rag.advisors.RagAdvisor;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Шаг RAG-конвейера, отвечающий за финальное обогащение и сборку промпта.
 * <p>
 * Этот шаг оркестрирует работу всех "советников" ({@link RagAdvisor}), которые
 * добавляют в модель промпта различный динамический контекст (текущая дата,
 * саммари истории и т.д.). После этого он использует {@link PromptService}
 * для рендеринга финального текста промпта из шаблона и модели.
 */
@Slf4j
@Component
@Order(38) // Выполняется после сжатия (35), но до генерации (40)
@RequiredArgsConstructor
public class AugmentationStep implements RagPipelineStep {

    private final List<RagAdvisor> advisors;
    private final PromptService promptService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [38] Augmentation: запуск обогащения и сборки финального промпта...");
        // 1. Запускаем все советники для обогащения модели промпта
        Mono<RagFlowContext> finalContextMono = Flux.fromIterable(advisors)
                .reduce(Mono.just(context), (contextMono, advisor) -> contextMono.flatMap(advisor::advise))
                .flatMap(mono -> mono);
        // 2. После работы советников, собираем финальный промпт
        return finalContextMono.map(finalContext -> {
            String structuredContext = finalContext.compressedContext();
            if (structuredContext == null || structuredContext.isBlank()) {
                structuredContext = "<no_relevant_context_found />";
            }
            finalContext.promptModel().put("structuredContext", structuredContext);
            finalContext.promptModel().put("question", finalContext.originalQuery());
            finalContext.promptModel().put("history", formatHistory(finalContext.history()));
            String promptString = promptService.render("ragPrompt", finalContext.promptModel());
            log.debug("Этап Augmentation успешно завершен. Финальный промпт собран.");
            return finalContext.withFinalPrompt(new Prompt(promptString));
        });
    }

    private String formatHistory(List<Message> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return "Нет истории.";
        }
        return chatHistory.stream()
                .map(m -> m.getMessageType().getValue() + ": " + m.getText())
                .collect(Collectors.joining("\n"));
    }
}
