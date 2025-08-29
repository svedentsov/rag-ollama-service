package com.example.ragollama.qaagent.impl;

import com.example.ragollama.indexing.IndexingPipelineService;
import com.example.ragollama.indexing.IndexingRequest;
import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.FeatureGapReport;
import com.example.ragollama.rag.domain.TestCaseService;
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
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Мета-агент, который анализирует рынок, сравнивая фичи своего продукта с конкурентом.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureGapAnalysisAgent implements ToolAgent {

    private final IndexingPipelineService indexingPipelineService;
    private final TestCaseService testCaseService; // Переиспользуем для RAG
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "feature-gap-analyzer";
    }

    @Override
    public String getDescription() {
        return "Сравнивает фичи нашего продукта с конкурентом и находит пробелы.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("scrapedText");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String competitorText = (String) context.payload().get("scrapedText");

        // Шаг 1: Индексируем контент конкурента "на лету"
        String competitorDocId = "competitor-" + UUID.randomUUID();
        indexingPipelineService.process(new IndexingRequest(competitorDocId, "Competitor Docs", competitorText, Map.of("doc_type", "competitor")));

        // Шаг 2: Извлекаем фичи из нашей базы знаний и из базы конкурента
        Mono<String> ourFeaturesMono = extractFeatures("Что умеет делать наш продукт?");
        Mono<String> competitorFeaturesMono = extractFeatures("Что умеет делать продукт, описанный в Competitor Docs?");

        // Шаг 3: Когда оба списка фичей готовы, передаем их на финальный анализ
        return Mono.zip(ourFeaturesMono, competitorFeaturesMono)
                .flatMap(tuple -> {
                    String promptString = promptService.render("featureGapAnalysis", Map.of(
                            "our_features_json", tuple.getT1(),
                            "competitor_features_json", tuple.getT2()
                    ));
                    return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED));
                })
                .map(this::parseLlmResponse)
                .map(report -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        report.summary(),
                        Map.of("featureGapReport", report)
                ))
                .toFuture();
    }

    private Mono<String> extractFeatures(String question) {
        // Используем RAG для получения релевантного контекста
        return testCaseService.findRelevantTestCases(question)
                .flatMap(contextDocs -> {
                    String context = contextDocs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
                    String promptString = promptService.render("featureExtraction", Map.of("context", context));
                    return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED));
                });
    }

    private FeatureGapReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, FeatureGapReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Feature Gap Analyzer LLM вернул невалидный JSON.", e);
        }
    }
}
