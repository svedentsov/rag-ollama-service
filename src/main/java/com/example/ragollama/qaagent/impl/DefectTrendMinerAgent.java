package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.domain.DefectRepository;
import com.example.ragollama.qaagent.model.Defect;
import com.example.ragollama.qaagent.model.DefectCluster;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * QA-агент, который анализирует исторические данные о дефектах,
 * кластеризует их по семантической схожести и генерирует отчет о трендах.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefectTrendMinerAgent implements QaAgent {

    private final DefectRepository defectRepository;
    private final VectorStore vectorStore;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "defect-trend-miner";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Кластеризует баг-репорты по семантической схожести и определяет общие темы/первопричины.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("days");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        Integer days = (Integer) context.payload().get("days");
        Double threshold = (Double) context.payload().get("clusterThreshold");
        List<Document> defects = defectRepository.findRecentDefects(days);

        if (defects.isEmpty()) {
            return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Дефекты за указанный период не найдены.", Map.of()));
        }

        List<DefectCluster> clusters = clusterDefects(defects, threshold);

        return Flux.fromIterable(clusters)
                .flatMap(this::summarizeCluster)
                .collectList()
                .map(summarizedClusters -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Анализ трендов дефектов завершен. Найдено " + summarizedClusters.size() + " кластеров.",
                        Map.of("defectClusters", summarizedClusters)
                ))
                .toFuture();
    }

    private List<DefectCluster> clusterDefects(List<Document> defects, double threshold) {
        List<DefectCluster> clusters = new ArrayList<>();
        Set<String> processedDocIds = new HashSet<>();

        for (Document seedDoc : defects) {
            if (processedDocIds.contains(seedDoc.getId())) {
                continue;
            }

            SearchRequest request = SearchRequest.builder()
                    .query(seedDoc.getText())
                    .topK(10)
                    .similarityThreshold(threshold)
                    .build();
            List<Document> neighbors = vectorStore.similaritySearch(request);

            List<Defect> clusterDefects = new ArrayList<>();
            clusterDefects.add(toDefect(seedDoc));
            processedDocIds.add(seedDoc.getId());

            for (Document neighbor : neighbors) {
                if (!processedDocIds.contains(neighbor.getId()) && defects.stream().anyMatch(d -> d.getId().equals(neighbor.getId()))) {
                    clusterDefects.add(toDefect(neighbor));
                    processedDocIds.add(neighbor.getId());
                }
            }

            if (clusterDefects.size() > 1) {
                clusters.add(new DefectCluster(clusterDefects.size(), clusterDefects));
            }
        }
        return clusters;
    }

    private Mono<DefectCluster> summarizeCluster(DefectCluster cluster) {
        String defectsText = cluster.getDefects().stream()
                // ИСПРАВЛЕНИЕ: Используем прямой доступ к полям record'а: defect.sourceName() и defect.content()
                .map(defect -> String.format("ID: %s\nТекст: %s\n", defect.sourceName(), defect.content()))
                .collect(Collectors.joining("---\n"));

        String promptString = promptService.render("defectTrendAnalyzer", Map.of("defectsText", defectsText));

        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                .map(summaryJson -> {
                    try {
                        String extractedJson = JsonExtractorUtil.extractJsonBlock(summaryJson);
                        Map<String, String> summaryMap = objectMapper.readValue(extractedJson, new TypeReference<>() {
                        });
                        cluster.setSummary(summaryMap.getOrDefault("theme", "Не удалось определить тему"));
                    } catch (Exception e) {
                        log.error("Не удалось распарсить резюме кластера: {}", summaryJson, e);
                        cluster.setSummary("Ошибка анализа");
                    }
                    return cluster;
                });
    }

    private Defect toDefect(Document doc) {
        return new Defect(
                doc.getId(),
                (String) doc.getMetadata().getOrDefault("source", "N/A"),
                doc.getText()
        );
    }
}
