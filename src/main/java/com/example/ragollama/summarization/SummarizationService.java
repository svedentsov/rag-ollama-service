package com.example.ragollama.summarization;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Сервис для создания краткого содержания (summary) текста.
 * <p>
 * Эта версия использует полностью асинхронный подход на базе Project Reactor,
 * что позволяет выполнять LLM-вызовы без блокировки потоков.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SummarizationService {

    private final LlmClient llmClient;
    private final PromptService promptService;

    /**
     * Асинхронно создает краткое содержание для предоставленного текста.
     *
     * @param text    Текст для анализа.
     * @param options Опции, управляющие стилем и форматом резюме.
     * @return {@link Mono}, который по завершении будет содержать строку с резюме.
     */
    public Mono<String> summarizeAsync(String text, SummaryOptions options) {
        if (text == null || text.isBlank()) {
            return Mono.just("Текст для анализа не предоставлен.");
        }

        log.info("Запущена задача саммаризации...");
        String style = Objects.requireNonNullElse(options, new SummaryOptions(null)).style();

        String promptString = promptService.render("summarizationPrompt", Map.of(
                "text", text,
                "style", style
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .map(tuple -> tuple.getT1());
    }

    public record SummaryOptions(String style) {
        public SummaryOptions {
            if (style == null || style.isBlank()) {
                style = "деловой и лаконичный";
            }
        }
    }
}
