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
import com.example.ragollama.shared.task.TaskLifecycleService;
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
 * Шаг RAG-конвейера "AI-Критик", отвечающий за валидацию сгенерированного ответа.
 * <p>
 * Этот шаг выполняется опционально (управляется через `application.yml`) и использует
 * LLM для проверки ответа на предмет галлюцинаций, полноты и корректности цитат.
 * Он добавляет отчет о валидации в финальный {@link RagAnswer}.
 */
@Component
@Order(70)
@Slf4j
@ConditionalOnProperty(name = "app.rag.validation.enabled", havingValue = "true")
public class ResponseValidationStep implements RagPipelineStep {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final TaskLifecycleService taskLifecycleService;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * Конструктор для внедрения всех необходимых зависимостей.
     *
     * @param llmClient              Клиент для взаимодействия с LLM.
     * @param promptService          Сервис для рендеринга шаблонов промптов.
     * @param objectMapper           Маппер для работы с JSON.
     * @param appProperties          Конфигурация приложения для условной активации.
     * @param taskLifecycleService   Сервис для управления задачами и отправки статусов.
     * @param jsonExtractorUtil      Утилита для надежного извлечения JSON из текста.
     */
    public ResponseValidationStep(
            LlmClient llmClient,
            PromptService promptService,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            TaskLifecycleService taskLifecycleService,
            JsonExtractorUtil jsonExtractorUtil
    ) {
        this.llmClient = llmClient;
        this.promptService = promptService;
        this.objectMapper = objectMapper;
        this.taskLifecycleService = taskLifecycleService;
        this.jsonExtractorUtil = jsonExtractorUtil;
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

        taskLifecycleService.getActiveTaskForSession(context.sessionId())
                .doOnNext(task -> taskLifecycleService.emitEvent(task.getId(), new UniversalResponse.StatusUpdate("Проверяю ответ на галлюцинации...")))
                .subscribe();

        String contextForPrompt = context.rerankedDocuments().stream()
                .map(doc -> String.format("<doc id=\"%s\">%s</doc>", doc.getMetadata().get("chunkId"), doc.getText()))
                .collect(Collectors.joining("\n\n"));

        String promptString = promptService.render("responseValidatorPrompt", Map.of(
                "question", context.originalQuery(),
                "context", contextForPrompt,
                "answer", context.finalAnswer().answer()
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
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
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ValidationReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Response Validator LLM вернул невалидный JSON.", e);
        }
    }
}
