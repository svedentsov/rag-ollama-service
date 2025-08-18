package com.example.ragollama.rag.domain;

import com.example.ragollama.rag.advisors.RagAdvisor;
import com.example.ragollama.rag.model.RagContext;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис, отвечающий за этап обогащения (Augmentation) в RAG-конвейере.
 */
@Service
@Slf4j
public class AugmentationService {

    private final List<RagAdvisor> advisors;
    private final ContextAssemblerService contextAssemblerService;
    private final PromptService promptService;

    public AugmentationService(List<RagAdvisor> advisors, ContextAssemblerService contextAssemblerService, PromptService promptService) {
        this.advisors = advisors;
        this.contextAssemblerService = contextAssemblerService;
        this.promptService = promptService;
        log.info("AugmentationService инициализирован с {} асинхронными советниками.", advisors.size());
    }

    public Mono<Prompt> augment(List<Document> documents, String query) {
        return augment(documents, query, Collections.emptyList());
    }

    public Mono<Prompt> augment(List<Document> documents, String query, List<Message> chatHistory) {
        RagContext initialContext = new RagContext(query);
        initialContext.setDocuments(documents);
        Mono<RagContext> initialContextMono = Mono.just(initialContext);
        Mono<RagContext> finalContextMono = Flux.fromIterable(advisors)
                .reduce(initialContextMono, (contextMono, advisor) -> contextMono.flatMap(advisor::advise))
                .flatMap(mono -> mono);
        return finalContextMono.map(finalContext -> {
            String contextString = contextAssemblerService.assembleContext(finalContext.getDocuments());
            finalContext.getPromptModel().put("context", contextString);
            finalContext.getPromptModel().put("question", query);
            String historyString = formatHistory(chatHistory);
            finalContext.getPromptModel().put("history", historyString);
            String promptString = promptService.createRagPrompt(finalContext.getPromptModel());
            log.debug("Этап Augmentation успешно завершен. История чата добавлена.");
            return new Prompt(promptString);
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
