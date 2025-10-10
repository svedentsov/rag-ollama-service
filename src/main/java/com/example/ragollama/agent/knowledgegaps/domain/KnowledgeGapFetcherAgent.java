package com.example.ragollama.agent.knowledgegaps.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.monitoring.domain.KnowledgeGapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Агент-инструмент для сбора данных о пробелах в знаниях, адаптированный для R2DBC.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeGapFetcherAgent implements ToolAgent {

    private final KnowledgeGapRepository knowledgeGapRepository;

    @Override
    public String getName() {
        return "knowledge-gap-fetcher";
    }

    @Override
    public String getDescription() {
        return "Извлекает из базы данных все запросы пользователей, на которые система не нашла ответ.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        log.info("Извлечение данных о пробелах в знаниях...");
        return knowledgeGapRepository.findAll()
                .map(com.example.ragollama.monitoring.model.KnowledgeGap::getQueryText)
                .collectList()
                .map(gapQueries -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Найдено " + gapQueries.size() + " запросов для анализа.",
                        Map.of("gapQueries", gapQueries)
                ));
    }
}
