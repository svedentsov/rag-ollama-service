package com.example.ragollama.rag.advisors;

import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Советник, отвечающий за интеллектуальное управление историей диалога.
 */
@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class HistorySummarizerAdvisor implements RagAdvisor {

    private final LlmClient llmClient;
    private final PromptService promptService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> advise(RagFlowContext context) {
        List<Message> history = context.history();

        if (history == null || history.isEmpty()) {
            context.promptModel().put("history_summary", "Это начало нового диалога.");
            return Mono.just(context);
        }

        String formattedHistory = history.stream()
                .map(m -> m.getMessageType().getValue().toUpperCase() + ": " + m.getText())
                .collect(Collectors.joining("\n"));

        String promptString = promptService.render("historySummarizerPrompt", Map.of("chat_history", formattedHistory));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.FASTEST)
                .map(summary -> {
                    context.promptModel().put("history_summary", summary);
                    log.debug("История диалога успешно суммаризирована.");
                    return context;
                })
                .onErrorResume(e -> {
                    log.error("Не удалось суммаризировать историю диалога. Используется fallback.", e);
                    context.promptModel().put("history_summary", "Не удалось обработать историю диалога.");
                    return Mono.just(context);
                });
    }
}
