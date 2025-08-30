package com.example.ragollama.summarization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.QaAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент-адаптер для сервиса саммаризации.
 * <p>
 * Связывает "чистый" {@link SummarizationService}, работающий на Project Reactor,
 * с платформой QA-агентов, которая ожидает {@link CompletableFuture}.
 * Этот класс является примером чистого архитектурного паттерна "Адаптер".
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SummarizerAgent implements QaAgent {

    /**
     * Ключ для извлечения текста из {@link AgentContext}.
     */
    public static final String TEXT_TO_SUMMARIZE_KEY = "textToSummarize";
    private final SummarizationService summarizationService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "summarizer-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Создает краткое содержание (summary) для предоставленного текста.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey(TEXT_TO_SUMMARIZE_KEY);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Метод асинхронно вызывает реактивный {@link SummarizationService},
     * преобразует результат с помощью оператора {@code map} и адаптирует
     * итоговый {@code Mono} к {@code CompletableFuture} для совместимости
     * с оркестратором агентов.
     *
     * @param context Контекст с входными данными для агента.
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * результат работы агента в виде {@link AgentResult}.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String text = (String) context.payload().get(TEXT_TO_SUMMARIZE_KEY);
        SummarizationService.SummaryOptions options = new SummarizationService.SummaryOptions(null);

        return summarizationService.summarizeAsync(text, options)
                .map(summary -> {
                    log.info("SummarizerAgent успешно сгенерировал summary.");
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Краткое содержание успешно создано.",
                            Map.of("summary", summary)
                    );
                })
                .toFuture();
    }
}
