package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> candidates = curationRepository.findDocumentsForCuration(10); // Обрабатываем по 10 за раз
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
