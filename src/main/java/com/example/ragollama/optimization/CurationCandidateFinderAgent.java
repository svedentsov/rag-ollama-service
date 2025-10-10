package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Агент, который находит кандидатов для курирования (улучшения метаданных).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CurationCandidateFinderAgent implements ToolAgent {
    private final VectorStoreCurationRepository curationRepository;

    @Override
    public String getName() {
        return "curation-candidate-finder";
    }

    @Override
    public String getDescription() {
        return "Находит документы в базе знаний, требующие курирования (например, без саммари).";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        return curationRepository.findDocumentsForCuration(10)
                .collectList()
                .map(candidates -> {
                    log.info("Найдено {} кандидатов для курирования.", candidates.size());
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Найдено кандидатов для курирования: " + candidates.size(),
                            Map.of("candidateIds", candidates)
                    );
                });
    }
}
