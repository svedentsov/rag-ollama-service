package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.task.CancellableTaskService;
import com.example.ragollama.shared.task.TaskStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Шаг RAG-конвейера, отвечающий за интеллектуальное сжатие контекста перед передачей его в LLM-генератор.
 * <p>
 * Перед началом выполнения отправляет статусное сообщение в UI.
 */
@Slf4j
@Component
@Order(35) // Выполняется после Reranking(25) и ContextExpansion(28), но до Augmentation(38)
@RequiredArgsConstructor
public class ContextualCompressionStep implements RagPipelineStep {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final TaskStateService taskStateService;
    private final CancellableTaskService taskService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        List<Document> documents = context.rerankedDocuments();
        if (documents.isEmpty()) {
            log.info("Шаг [35] Context Compression: пропущен, нет документов для сжатия.");
            return Mono.just(context.withCompressedContext(""));
        }
        log.info("Шаг [35] Context Compression: запуск сжатия {} документов...", documents.size());
        taskStateService.getActiveTaskIdForSession(context.sessionId()).ifPresent(taskId ->
                taskService.emitEvent(taskId, new UniversalResponse.StatusUpdate("Сжимаю найденную информацию...")));
        String documentsForPrompt = documents.stream()
                .map(doc -> String.format("<doc id=\"%s\">\n%s\n</doc>",
                        doc.getMetadata().get("chunkId"), doc.getText()))
                .collect(Collectors.joining("\n\n"));
        String promptString = promptService.render("contextCompressorPrompt", Map.of(
                "question", context.originalQuery(),
                "documents", documentsForPrompt
        ));
        // Для сжатия используем быструю модель, так как это задача извлечения, а не рассуждения
        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.FASTEST))
                .map(compressedText -> {
                    log.info("Контекст успешно сжат.");
                    return context.withCompressedContext(compressedText);
                });
    }
}
