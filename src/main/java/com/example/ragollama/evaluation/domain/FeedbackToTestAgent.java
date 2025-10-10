package com.example.ragollama.evaluation.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.evaluation.model.FeedbackAnalysisResult;
import com.example.ragollama.evaluation.model.GoldenRecord;
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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * AI-агент, который преобразует негативный фидбэк пользователя в новый
 * тест для "золотого датасета".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackToTestAgent implements ToolAgent {

    private final FeedbackAnalysisService feedbackAnalysisService;
    private final GoldenRecordRepository goldenRecordRepository;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "feedback-to-test-converter";
    }

    @Override
    public String getDescription() {
        return "Анализирует негативный фидбэк и создает новый тест для 'золотого датасета'.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().get("feedbackId") instanceof UUID;
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        UUID feedbackId = (UUID) context.payload().get("feedbackId");

        return feedbackAnalysisService.getContextForFeedback(feedbackId)
                .switchIfEmpty(Mono.error(new ProcessingException("Контекст для feedbackId " + feedbackId + " не найден.")))
                .flatMap(feedbackContext -> {
                    String promptString = promptService.render("feedbackToTestPrompt", Map.of(
                            "user_query", feedbackContext.originalQuery(),
                            "bad_ai_answer", feedbackContext.badAnswer(),
                            "user_comment", feedbackContext.userComment(),
                            "retrieved_docs", feedbackContext.retrievedDocumentIds()
                    ));
                    return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                            .map(llmResponse -> createGoldenRecord(llmResponse, feedbackContext));
                });
    }

    private AgentResult createGoldenRecord(String llmResponse, FeedbackAnalysisService.FeedbackContext context) {
        try {
            FeedbackAnalysisResult analysis = parseLlmResponse(llmResponse);
            if (analysis.correctedDocumentIds() == null || analysis.correctedDocumentIds().isEmpty()) {
                String message = "AI-аналитик не смог определить корректный набор документов. Новый тест не создан.";
                log.warn(message);
                return new AgentResult(getName(), AgentResult.Status.SUCCESS, message, Map.of());
            }
            GoldenRecord newRecord = new GoldenRecord(
                    "feedback-" + UUID.randomUUID().toString().substring(0, 8),
                    context.originalQuery(),
                    new HashSet<>(analysis.correctedDocumentIds())
            );
            goldenRecordRepository.save(newRecord);
            return new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    "Новый тест успешно создан и добавлен в 'золотой датасет'.",
                    Map.of("newGoldenRecord", newRecord)
            );
        } catch (IOException e) {
            throw new ProcessingException("Не удалось сохранить новый GoldenRecord.", e);
        }
    }

    private FeedbackAnalysisResult parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, FeedbackAnalysisResult.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Feedback-to-Test LLM вернул невалидный JSON.", e);
        }
    }
}
