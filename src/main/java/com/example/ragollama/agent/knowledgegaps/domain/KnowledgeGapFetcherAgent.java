package com.example.ragollama.agent.knowledgegaps.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.monitoring.domain.KnowledgeGapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент-инструмент для сбора "сырых" данных о пробелах в знаниях из базы данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeGapFetcherAgent implements ToolAgent {

    private final KnowledgeGapRepository knowledgeGapRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "knowledge-gap-fetcher";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Извлекает из базы данных все запросы пользователей, на которые система не нашла ответ.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Извлечение данных о пробелах в знаниях...");
            List<String> gapQueries = knowledgeGapRepository.findAll().stream()
                    .map(com.example.ragollama.monitoring.model.KnowledgeGap::getQueryText)
                    .toList();

            return new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    "Найдено " + gapQueries.size() + " запросов для анализа.",
                    Map.of("gapQueries", gapQueries)
            );
        });
    }
}
