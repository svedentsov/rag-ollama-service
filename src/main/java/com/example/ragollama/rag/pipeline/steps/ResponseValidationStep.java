package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.evaluation.model.ValidationReport;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.domain.model.RagAnswer;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.task.CancellableTaskService;
import com.example.ragollama.shared.task.TaskStateService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Шаг RAG-конвейера, выполняющий роль "AI-Критика".
 * <p>
 * Этот шаг выполняется после генерации ответа и использует LLM для
 * валидации сгенерированного текста на предмет галлюцинаций, полноты
 * и корректности цитирования.
 * <p>
 * Активируется свойством {@code app.rag.validation.enabled=true}.
 */
@Component
@Order(70) // Выполняется после Trust Scoring (60)
@Slf4j
@ConditionalOnProperty(name = "app.rag.validation.enabled", havingValue = "true")
public class ResponseValidationStep implements RagPipelineStep {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final TaskStateService taskStateService;
    private final CancellableTaskService taskService;

    public ResponseValidationStep(LlmClient llmClient, PromptService promptService, ObjectMapper objectMapper, AppProperties appProperties, TaskStateService taskStateService, CancellableTaskService taskService) {
        this.llmClient = llmClient;
        this.promptService = promptService;
        this.objectMapper = objectMapper;
        this.taskStateService = taskStateService;
        this.taskService = taskService;
        if (appProperties.rag().validation().enabled()) {
            log.info("Активирован шаг конвейера: ResponseValidationStep (AI-Критик)");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        if (context.finalAnswer() == null || context.finalAnswer().answer().isBlank() || context.rerankedDocuments().isEmpty()) {
            return Mono.just(context);
        }
        log.info("Шаг [70] Response Validation: запуск AI-критика...");
        taskStateService.getActiveTaskIdForSession(context.sessionId()).ifPresent(taskId ->
                taskService.emitEvent(taskId, new UniversalResponse.StatusUpdate("Проверяю ответ на галлюцинации...")));
        String contextForPrompt = context.rerankedDocuments().stream()
                .map(doc -> String.format("<doc id=\"%s\">%s</doc>", doc.getMetadata().get("chunkId"), doc.getText()))
                .collect(Collectors.joining("\n\n"));
        String promptString = promptService.render("responseValidatorPrompt", Map.of(
                "question", context.originalQuery(),
                "context", contextForPrompt,
                "answer", context.finalAnswer().answer()
        ));
        // Для задачи критики используем самую мощную модель
        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true))
                .map(this::parseLlmResponse)
                .map(validationReport -> {
                    if (!validationReport.isValid()) {
                        log.warn("!!! AI-Критик обнаружил проблемы в ответе: {}", validationReport.findings());
                    } else {
                        log.info("AI-Критик подтвердил высокое качество ответа.");
                    }
                    RagAnswer originalAnswer = context.finalAnswer();
                    RagAnswer finalAnswer = new RagAnswer(
                            originalAnswer.answer(),
                            originalAnswer.sourceCitations(),
                            originalAnswer.trustScoreReport(),
                            validationReport
                    );
                    return context.withFinalAnswer(finalAnswer);
                });
    }

    private ValidationReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ValidationReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Response Validator LLM вернул невалидный JSON.", e);
        }
    }
}
