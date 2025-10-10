package com.example.ragollama.summarization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.QaAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Агент-адаптер для сервиса саммаризации.
 * <p>
 * Связывает "чистый" {@link SummarizationService}, работающий на Project Reactor,
 * с платформой QA-агентов.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SummarizerAgent implements QaAgent {

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
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
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
                });
    }
}
