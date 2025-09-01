package com.example.ragollama.optimization;

import com.example.ragollama.evaluation.RagEvaluationService;
import com.example.ragollama.evaluation.model.EvaluationResult;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class IsolatedEvaluationRunner {

    private final RagEvaluationService evaluationService;
    private final RetrievalProperties defaultRetrievalProperties;

    public Mono<EvaluationResult> runEvaluationWithOverrides(Map<String, Object> overrides) {
        return evaluationService.evaluate();
    }
}
