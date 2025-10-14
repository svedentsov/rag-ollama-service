package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.domain.reranking.RerankingService;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.task.TaskLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Шаг RAG-конвейера, отвечающий за переранжирование найденных документов
 * для повышения их итоговой релевантности.
 */
@Component
@Order(25)
@Slf4j
@RequiredArgsConstructor
public class RerankingStep implements RagPipelineStep {

    private final RerankingService rerankingService;
    private final TaskLifecycleService taskLifecycleService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        if (context.retrievedDocuments().isEmpty()) {
            return Mono.just(context);
        }
        log.info("Шаг [25] Reranking: запуск переранжирования {} документов...", context.retrievedDocuments().size());

        taskLifecycleService.getActiveTaskForSession(context.sessionId())
                .doOnNext(task -> taskLifecycleService.emitEvent(task.getId(), new UniversalResponse.StatusUpdate("Оцениваю релевантность найденного...")))
                .subscribe();

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
