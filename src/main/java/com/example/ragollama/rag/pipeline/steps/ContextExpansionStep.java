package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
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
 * Заменяет найденные дочерние чанки на их полные родительские документы для обогащения контекста.
 */
@Component
@Order(28)
@Slf4j
@RequiredArgsConstructor
public class ContextExpansionStep implements RagPipelineStep {

    /**
     * {@inheritDoc}
     * <p>
     * Принимает список отранжированных "дочерних" документов, извлекает из их
     * метаданных полные "родительские" документы и заменяет ими исходный список.
     *
     * @param context Текущий контекст RAG-конвейера.
     * @return {@link Mono} с обновленным контекстом, содержащим родительские документы.
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [28] Context Expansion: замена дочерних чанков на родительские...");

        List<Document> rerankedChildDocs = context.rerankedDocuments();
        if (rerankedChildDocs.isEmpty()) {
            return Mono.just(context);
        }

        // Группируем по parentChunkId и создаем родительские документы, сохраняя уникальность
        Map<String, Document> parentDocsMap = rerankedChildDocs.stream()
                .filter(doc -> doc.getMetadata().containsKey("parentChunkId") && doc.getMetadata().containsKey("parentChunkText"))
                .collect(Collectors.toMap(
                        doc -> (String) doc.getMetadata().get("parentChunkId"),
                        this::createParentDocumentFromChild,
                        (existing, replacement) -> existing // В случае дубликата оставляем первый
                ));

        if (parentDocsMap.isEmpty()) {
            log.warn("Не найдено ни одного чанка с метаданными родителя. Пропускаем шаг расширения контекста.");
            return Mono.just(context);
        }

        List<Document> parentDocs = List.copyOf(parentDocsMap.values());
        log.info("Заменено {} дочерних чанков на {} уникальных родительских.", rerankedChildDocs.size(), parentDocs.size());

        // Обновляем rerankedDocuments, так как следующий шаг будет работать с ними
        return Mono.just(context.withRerankedDocuments(parentDocs));
    }

    private Document createParentDocumentFromChild(Document childDoc) {
        Map<String, Object> parentMetadata = new HashMap<>(childDoc.getMetadata());
        String parentChunkId = (String) childDoc.getMetadata().get("parentChunkId");
        String parentText = (String) childDoc.getMetadata().get("parentChunkText");

        // Удаляем специфичные для дочернего элемента метаданные
        parentMetadata.remove("parentChunkId");
        parentMetadata.remove("parentChunkText");
        // Устанавливаем ID родителя как основной
        parentMetadata.put("chunkId", parentChunkId);
        return new Document(parentChunkId, parentText, parentMetadata);
    }
}