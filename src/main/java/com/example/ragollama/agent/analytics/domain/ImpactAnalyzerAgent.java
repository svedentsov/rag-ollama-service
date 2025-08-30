package com.example.ragollama.agent.analytics.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.analytics.model.ImpactAnalysis;
import com.example.ragollama.agent.git.tools.GitApiClient;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент для проведения анализа влияния (Impact Analysis) изменений в коде.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImpactAnalyzerAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "impact-analyzer";
    }

    @Override
    public String getDescription() {
        return "Анализирует изменения в коде и прогнозирует их влияние на другие компоненты системы.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles") && context.payload().containsKey("newRef");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<String> changedFiles = (List<String>) context.payload().get("changedFiles");
        String newRef = (String) context.payload().get("newRef");

        log.info("ImpactAnalyzerAgent: анализ влияния для {} измененных файлов в ref '{}'", changedFiles.size(), newRef);

        return Flux.fromIterable(changedFiles)
                .filter(filePath -> filePath.endsWith(".java") && filePath.contains("src/main/java"))
                .flatMap(filePath -> gitApiClient.getFileContent(filePath, newRef)
                        .flatMap(content -> analyzeFileImpact(content, filePath))
                        .onErrorResume(e -> {
                            log.error("Ошибка при анализе влияния для файла {}: {}", filePath, e.getMessage());
                            return Mono.empty();
                        }))
                .collectList()
                .map(allImpacts -> {
                    List<ImpactAnalysis> flattenedImpacts = allImpacts.stream().flatMap(List::stream).toList();
                    String summary = String.format("Анализ влияния завершен. Найдено %d потенциальных точек влияния.", flattenedImpacts.size());
                    log.info(summary);

                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            summary,
                            Map.of("impacts", flattenedImpacts)
                    );
                })
                .toFuture();
    }

    /**
     * Вызывает LLM для анализа одного файла.
     */
    private Mono<List<ImpactAnalysis>> analyzeFileImpact(String code, String filePath) {
        if (code == null || code.isBlank()) {
            return Mono.just(List.of());
        }

        String promptString = promptService.render("impactAnalysis", Map.of("code", code, "filePath", filePath));
        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                .map(this::parseLlmResponse);
    }

    /**
     * Безопасно парсит JSON-ответ от LLM.
     */
    private List<ImpactAnalysis> parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonResponse.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
            if (cleanedJson.isEmpty() || cleanedJson.equals("[]")) {
                return List.of();
            }
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM для ImpactAnalysis: {}", jsonResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON для ImpactAnalysis.", e);
        }
    }
}
