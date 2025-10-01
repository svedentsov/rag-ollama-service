package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.task.TaskLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Шаг RAG-конвейера, реализующий логику Parent Document Retriever.
 */
@Component
@Order(28)
@Slf4j
@RequiredArgsConstructor
public class ContextExpansionStep implements RagPipelineStep {

    private final TaskLifecycleService taskLifecycleService;

    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [28] Context Expansion: замена дочерних чанков на родительские...");

        List<Document> rerankedChildDocs = context.rerankedDocuments();
        if (rerankedChildDocs.isEmpty()) {
            return Mono.just(context);
        }
        taskLifecycleService.getActiveTaskForSession(context.sessionId()).ifPresent(task ->
                taskLifecycleService.emitEvent(task.getId(), new UniversalResponse.StatusUpdate("Расширяю контекст...")));
        Map<String, Document> parentDocsMap = rerankedChildDocs.stream()
                .filter(doc -> doc.getMetadata().containsKey("parentChunkId") && doc.getMetadata().containsKey("parentChunkText"))
                .collect(Collectors.toMap(
                        doc -> (String) doc.getMetadata().get("parentChunkId"),
                        this::createParentDocumentFromChild,
                        (existing, replacement) -> existing
                ));
        if (parentDocsMap.isEmpty()) {
            log.warn("Не найдено ни одного чанка с метаданными родителя. Пропускаем шаг расширения контекста.");
            return Mono.just(context);
        }
        List<Document> parentDocs = List.copyOf(parentDocsMap.values());
        log.info("Заменено {} дочерних чанков на {} уникальных родительских.", rerankedChildDocs.size(), parentDocs.size());
        return Mono.just(context.withRerankedDocuments(parentDocs));
    }

    private Document createParentDocumentFromChild(Document childDoc) {
        Map<String, Object> parentMetadata = new HashMap<>(childDoc.getMetadata());
        String parentChunkId = (String) childDoc.getMetadata().get("parentChunkId");
        String parentText = (String) childDoc.getMetadata().get("parentChunkText");
        parentMetadata.remove("parentChunkId");
        parentMetadata.remove("parentChunkText");
        parentMetadata.put("chunkId", parentChunkId);
        return new Document(parentChunkId, parentText, parentMetadata);
    }
}
