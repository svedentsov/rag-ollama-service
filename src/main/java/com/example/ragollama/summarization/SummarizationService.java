package com.example.ragollama.summarization;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class SummarizationService {

    private final LlmClient llmClient;
    private final PromptService promptService;

    public CompletableFuture<String> summarizeAsync(String text, SummaryOptions options) {
        if (text == null || text.isBlank()) {
            return CompletableFuture.completedFuture("Текст для анализа не предоставлен.");
        }

        log.info("Запущена задача саммаризации...");
        String style = Objects.requireNonNullElse(options, new SummaryOptions(null)).style();

        String promptString = promptService.render("summarization", Map.of(
                "text", text,
                "style", style
        ));

        return llmClient.callChat(new Prompt(promptString));
    }

    /**
     * DTO для опций саммаризации.
     *
     * @param style Стиль изложения (например, "деловой", "в виде списка", "простой").
     */
    public record SummaryOptions(String style) {
        /**
         * Компактный конструктор для установки значения по умолчанию.
         * Он вызывается автоматически при вызове канонического конструктора.
         */
        public SummaryOptions {
            if (style == null || style.isBlank()) {
                style = "деловой и лаконичный";
            }
        }
    }
}
