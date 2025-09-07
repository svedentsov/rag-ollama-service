package com.example.ragollama.agent.analytics.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.buganalysis.domain.BugReportHistoryService;
import com.example.ragollama.agent.analytics.model.BugPattern;
import com.example.ragollama.agent.analytics.model.BugPatternReport;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который анализирует историю баг-репортов, кластеризует их
 * и выявляет повторяющиеся системные проблемы (паттерны).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BugPatternDetectorAgent implements ToolAgent {

    private final BugReportHistoryService bugReportHistoryService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "bug-pattern-detector";
    }

    @Override
    public String getDescription() {
        return "Анализирует историю багов, кластеризует их и выявляет системные паттерны проблем.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("analysisPeriodDays");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        // Параметр analysisPeriodDays пока не используется, но оставлен для будущего
        return bugReportHistoryService.fetchAllBugReports()
                .flatMap(allBugs -> {
                    if (allBugs.size() < 10) { // Не запускаем анализ, если данных слишком мало
                        return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Недостаточно данных для анализа паттернов.", Map.of()));
                    }
                    List<List<Document>> clusters = clusterBugs(allBugs);
                    return analyzeClusters(clusters);
                })
                .toFuture();
    }

    private List<List<Document>> clusterBugs(List<Document> allBugs) {
        // Mock-логика для кластеризации. В реальной системе здесь будет K-Means или DBSCAN.
        // Для демонстрации, просто разделим на N случайных кластеров.
        log.info("Кластеризация {} баг-репортов...", allBugs.size());
        Collections.shuffle(allBugs);
        int numberOfClusters = Math.max(2, allBugs.size() / 5);
        List<List<Document>> clusters = new ArrayList<>();
        int clusterSize = allBugs.size() / numberOfClusters;
        for (int i = 0; i < numberOfClusters; i++) {
            int start = i * clusterSize;
            int end = Math.min((i + 1) * clusterSize, allBugs.size());
            if (start < end) {
                clusters.add(allBugs.subList(start, end));
            }
        }
        log.info("Создано {} кластеров для анализа.", clusters.size());
        return clusters;
    }

    private Mono<AgentResult> analyzeClusters(List<List<Document>> clusters) {
        return Flux.fromIterable(clusters)
                .flatMap(this::analyzeSingleCluster)
                .collectList()
                .map(patterns -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Анализ паттернов завершен. Найдено системных проблем: " + patterns.size(),
                        Map.of("bugPatternReport", new BugPatternReport(patterns))
                ));
    }

    private Mono<BugPattern> analyzeSingleCluster(List<Document> cluster) {
        try {
            String clusterJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    cluster.stream().map(doc -> Map.of("id", doc.getMetadata().get("documentId"), "content", doc.getText())).toList()
            );
            String promptString = promptService.render("bugPatternDetectorPrompt", Map.of("bug_cluster_json", clusterJson));

            return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                    .map(this::parseLlmResponse);
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации кластера багов", e));
        }
    }

    private BugPattern parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, BugPattern.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Bug Pattern Detector LLM вернул невалидный JSON.", e);
        }
    }
}
