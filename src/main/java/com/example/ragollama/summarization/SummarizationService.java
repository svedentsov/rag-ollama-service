package com.example.ragollama.summarization;

import com.example.ragollama.shared.llm.LlmClient;
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
 * Эта версия использует полностью асинхронный подход на базе Project Reactor.
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

        String promptString = promptService.render("summarization", Map.of(
                "text", text,
                "style", style
        ));

        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString)));
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
