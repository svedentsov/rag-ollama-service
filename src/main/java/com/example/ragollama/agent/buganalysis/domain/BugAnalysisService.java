package com.example.ragollama.agent.buganalysis.domain;

import com.example.ragollama.agent.buganalysis.api.dto.BugAnalysisResponse;
import com.example.ragollama.agent.buganalysis.mappers.BugAnalysisMapper;
import com.example.ragollama.rag.pipeline.RagPipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.UUID;

/**
 * Сервис-оркестратор для агента анализа баг-репортов.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BugAnalysisService {

    private final RagPipelineOrchestrator ragPipelineOrchestrator;
    private final BugAnalysisMapper bugAnalysisMapper;

    /**
     * Асинхронно анализирует баг-репорт, используя унифицированный RAG-конвейер.
     * <p>
     * Метод принимает "сырое" описание бага, передает его в основной
     * RAG-оркестратор для поиска похожих документов и генерации анализа,
     * а затем преобразует "сырой" JSON-ответ от LLM в строго типизированный
     * DTO {@link BugAnalysisResponse} с помощью маппера.
     *
     * @param draftDescription Черновик описания бага от пользователя.
     * @return {@link Mono}, который по завершении будет содержать
     * структурированный {@link BugAnalysisResponse}.
     */
    public Mono<BugAnalysisResponse> analyzeBugReport(String draftDescription) {
        log.info("Запуск анализа бага для: '{}'", draftDescription);
        return Mono.fromFuture(ragPipelineOrchestrator.queryAsync(
                        draftDescription,
                        Collections.emptyList(),
                        4, // default topK
                        0.7, // default threshold
                        UUID.randomUUID() // Создаем временную сессию для этого анализа
                ))
                .map(ragAnswer -> bugAnalysisMapper.parse(ragAnswer.answer()));
    }
}
