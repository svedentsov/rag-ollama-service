package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Шаг RAG-конвейера, отвечающий за интеллектуальное сжатие контекста
 * перед передачей его в LLM-генератор.
 * <p> <b>ВАЖНО:</b> В текущей версии функциональность сжатия с помощью LLM временно
 * отключена из-за проблем с потерей релевантной информации. Шаг работает в
 * режиме "pass-through", просто форматируя документы в строку без их изменения.
 */
@Slf4j
@Component
@Order(35)
@RequiredArgsConstructor
public class ContextualCompressionStep implements RagPipelineStep {

    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        List<Document> documents = context.rerankedDocuments();
        if (documents.isEmpty()) {
            log.info("Шаг [35] Context Compression: пропущен, нет документов для сжатия.");
            return Mono.just(context.withCompressedContext(""));
        }
        log.warn("Шаг [35] Context Compression: работает в режиме PASS-THROUGH. Сжатие с помощью LLM отключено.");
        String documentsForPrompt = documents.stream()
                .map(doc -> String.format("<doc id=\"%s\">\n%s\n</doc>",
                        doc.getMetadata().get("chunkId"), doc.getText()))
                .collect(Collectors.joining("\n\n"));
        return Mono.just(context.withCompressedContext(documentsForPrompt));
    }
}
