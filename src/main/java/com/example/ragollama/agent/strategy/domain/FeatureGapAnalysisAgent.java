package com.example.ragollama.agent.strategy.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.strategy.model.FeatureGapReport;
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
import java.util.stream.Collectors;

/**
 * Мета-агент, который анализирует рынок, сравнивая фичи своего продукта с конкурентом.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureGapAnalysisAgent implements ToolAgent {

    private final TestCaseService testCaseService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "feature-gap-analyzer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Сравнивает фичи нашего продукта с конкурентом и находит пробелы.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("scrapedText");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String competitorText = (String) context.payload().get("scrapedText");

        Mono<String> ourFeaturesMono = extractFeaturesFromOurKnowledgeBase();
        Mono<String> competitorFeaturesMono = extractFeaturesFromText(competitorText);

        return Mono.zip(ourFeaturesMono, competitorFeaturesMono)
                .flatMap(tuple -> {
                    String promptString = promptService.render("featureGapAnalysisPrompt", Map.of(
                            "our_features_json", tuple.getT1(),
                            "competitor_features_json", tuple.getT2()
                    ));
                    return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true);
                })
                .map(tuple -> parseLlmResponse(tuple.getT1()))
                .map(report -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        report.summary(),
                        Map.of("featureGapReport", report)
                ));
    }

    private Mono<String> extractFeaturesFromOurKnowledgeBase() {
        return testCaseService.findRelevantTestCases("Все возможности продукта")
                .flatMap(contextDocs -> {
                    String context = contextDocs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
                    return extractFeaturesFromText(context);
                });
    }

    private Mono<String> extractFeaturesFromText(String text) {
        String promptString = promptService.render("featureExtractionPrompt", Map.of("context", text));
        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                .map(tuple -> tuple.getT1());
    }

    private FeatureGapReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, FeatureGapReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Feature Gap Analyzer LLM вернул невалидный JSON.", e);
        }
    }
}
