package com.example.ragollama.agent.buganalysis.domain;

import com.example.ragollama.agent.buganalysis.api.dto.BugAnalysisResponse;
import com.example.ragollama.agent.buganalysis.mappers.BugAnalysisMapper;
import com.example.ragollama.rag.agent.QueryProcessingPipeline;
import com.example.ragollama.rag.domain.AugmentationService;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Сервис-оркестратор для агента анализа баг-репортов.
 * <p>
 * Эта отрефакторенная версия является "чистым" оркестратором. Она не содержит
 * деталей реализации этапов RAG, а лишь вызывает специализированные сервисы
 * в нужной последовательности, следуя принципам Clean Architecture.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BugAnalysisService {

    private final QueryProcessingPipeline queryProcessingPipeline;
    private final HybridRetrievalStrategy retrievalStrategy;
    private final AugmentationService augmentationService;
    private final LlmClient llmClient;
    private final BugAnalysisMapper bugAnalysisMapper;
    private final RetrievalProperties retrievalProperties;

    /**
     * Асинхронно анализирует баг-репорт, используя неблокирующий RAG-конвейер.
     *
     * @param draftDescription Черновик описания бага от пользователя.
     * @return {@link Mono}, который по завершении будет содержать
     * структурированный {@link BugAnalysisResponse}.
     */
    public Mono<BugAnalysisResponse> analyzeBugReport(String draftDescription) {
        log.info("Запуск анализа бага для: '{}'", draftDescription);
        // 1. Подготовка: определяем фильтр и параметры поиска
        Filter.Expression filter = new Filter.Expression(
                Filter.ExpressionType.EQ, new Filter.Key("metadata.doc_type"), new Filter.Value("bug_report")
        );
        var retrievalConfig = retrievalProperties.hybrid().vectorSearch();
        // 2. Запускаем асинхронный RAG-конвейер
        return queryProcessingPipeline.process(draftDescription)
                .flatMap(processedQueries -> retrievalStrategy.retrieve(
                        processedQueries,
                        draftDescription,
                        retrievalConfig.topK(),
                        retrievalConfig.similarityThreshold(),
                        filter
                ))
                .flatMap(similarDocs -> augmentationService.augment(similarDocs, draftDescription, null))
                .flatMap(prompt -> Mono.fromFuture(llmClient.callChat(prompt, ModelCapability.BALANCED)))
                .map(bugAnalysisMapper::parse);
    }
}
