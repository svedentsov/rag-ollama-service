package com.example.ragollama.rag.postprocessing;

import com.example.ragollama.optimization.SourceAnalyzerService;
import com.example.ragollama.optimization.model.TrustScoreReport;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.metrics.MetricService;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(40) // Выполняется одним из последних
@RequiredArgsConstructor
public class TrustScoringPostProcessor implements RagPostProcessor {

    private final SourceAnalyzerService sourceAnalyzer;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final MetricService metricService;
    private final ObjectMapper objectMapper;

    @Override
    public CompletableFuture<Void> process(RagProcessingContext context) {
        if (context.documents().isEmpty()) {
            metricService.recordTrustScore(0);
            return CompletableFuture.completedFuture(null);
        }
        // 1. Детерминированный анализ источников
        int recencyScore = sourceAnalyzer.analyzeRecency(context.documents());
        int authorityScore = sourceAnalyzer.analyzeAuthority(context.documents());
        // 2. AI-анализ уверенности
        String contextAsString = context.documents().stream()
                .map(doc -> String.format("<doc source=\"%s\">\n%s\n</doc>",
                        doc.getMetadata().get("source"), doc.getText()))
                .collect(Collectors.joining("\n\n"));
        String promptString = promptService.render("trustScorer", Map.of(
                "context", contextAsString,
                "question", context.originalQuery(),
                "answer", context.response().answer()
        ));
        return llmClient.callChat(new Prompt(promptString), ModelCapability.FAST)
                .thenAccept(llmResponse -> {
                    TrustScoreReport partialReport = parseLlmResponse(llmResponse);
                    // 3. Комбинируем все оценки в финальный скор
                    int finalScore = calculateFinalScore(partialReport.confidenceScore(), recencyScore, authorityScore);
                    TrustScoreReport fullReport = new TrustScoreReport(
                            finalScore, partialReport.confidenceScore(), recencyScore,
                            authorityScore, partialReport.justification()
                    );
                    log.info("Оценка доверия для запроса '{}': {}", context.originalQuery(), fullReport);
                    metricService.recordTrustScore(finalScore);
                }).exceptionally(ex -> {
                    log.error("Ошибка при вычислении Trust Score", ex);
                    return null;
                });
    }

    private int calculateFinalScore(int confidence, int recency, int authority) {
        return (int) (confidence * 0.6 + recency * 0.2 + authority * 0.2);
    }

    private TrustScoreReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, TrustScoreReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("TrustScorer LLM вернул невалидный JSON.", e);
        }
    }
}
