package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.domain.reranking.RerankingService;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.task.CancellableTaskService;
import com.example.ragollama.shared.task.TaskStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Шаг RAG-конвейера, отвечающий за интеллектуальное переранжирование
 * извлеченных документов для повышения релевантности.
 * <p>
 * Этот шаг делегирует всю сложную логику специализированному
 * {@link RerankingService}, который, в свою очередь, применяет
 * одну или несколько сконфигурированных стратегий переранжирования.
 * <p>
 * Перед началом выполнения отправляет статусное сообщение в UI.
 *
 * @see RerankingService
 */
@Component
@Order(25) // Выполняется сразу после Retrieval (20)
@Slf4j
@RequiredArgsConstructor
public class RerankingStep implements RagPipelineStep {

    private final RerankingService rerankingService;
    private final TaskStateService taskStateService;
    private final CancellableTaskService taskService;

    /**
     * {@inheritDoc}
     * <p>
     * Асинхронно выполняет переранжирование, предварительно уведомив UI о начале этого этапа.
     *
     * @param context Текущий контекст выполнения, содержащий извлеченные документы.
     * @return {@link Mono} с обновленным контекстом, содержащим переранжированные документы.
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        if (context.retrievedDocuments().isEmpty()) {
            return Mono.just(context); // Пропускаем шаг, если нечего ранжировать
        }
        log.info("Шаг [25] Reranking: запуск переранжирования {} документов...", context.retrievedDocuments().size());
        taskStateService.getActiveTaskIdForSession(context.sessionId()).ifPresent(taskId ->
                taskService.emitEvent(taskId, new UniversalResponse.StatusUpdate("Оцениваю релевантность найденного...")));
        return Mono.fromCallable(() -> {
            List<Document> reranked = rerankingService.rerank(
                    context.retrievedDocuments(),
                    context.originalQuery()
            );
            log.debug("Переранжирование завершено. Количество документов: {}", reranked.size());
            return context.withRerankedDocuments(reranked);
        });
    }
}
