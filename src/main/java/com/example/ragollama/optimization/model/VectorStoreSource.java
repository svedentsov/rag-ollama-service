package com.example.ragollama.optimization.model;

import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Реализация {@link KnowledgeSource}, которая ищет доказательства в векторной базе данных.
 */
@Component
@RequiredArgsConstructor
public class VectorStoreSource implements KnowledgeSource {

    private final QueryProcessingPipeline queryProcessingPipeline;
    private final HybridRetrievalStrategy retrievalStrategy;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSourceName() {
        return "VectorStore (Документация)";
    }

    /**
     * {@inheritDoc}
     *
     * @param claim Утверждение для проверки.
     * @return {@link Mono} со списком текстовых содержимых найденных документов.
     */
    @Override
    public Mono<List<String>> findEvidence(String claim) {
        return queryProcessingPipeline.process(claim)
                .flatMap(queries -> retrievalStrategy.retrieve(queries, claim, 3, 0.8, null))
                .map(docs -> docs.stream().map(Document::getText).toList());
    }
}
