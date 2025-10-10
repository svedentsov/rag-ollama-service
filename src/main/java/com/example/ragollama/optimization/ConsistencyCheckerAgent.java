package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.KnowledgeSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AI-агент, который координирует сбор "доказательств" из различных
 * источников знаний для проверки заданного утверждения.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsistencyCheckerAgent implements ToolAgent {

    private final List<KnowledgeSource> knowledgeSources;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "consistency-checker";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Собирает доказательства из всех источников знаний для проверки утверждения.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("claim");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String claim = (String) context.payload().get("claim");
        log.info("Запуск сбора доказательств для утверждения: '{}'", claim);

        return Flux.fromIterable(knowledgeSources)
                .flatMap(source -> source.findEvidence(claim)
                        .map(evidenceList -> Map.entry(source.getSourceName(), evidenceList)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(allEvidence -> {
                    log.info("Сбор доказательств завершен.");
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Сбор доказательств завершен.",
                            Map.of("allEvidence", allEvidence)
                    );
                });
    }
}
