package com.example.ragollama.rag.postprocessing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис-оркестратор, который асинхронно запускает все доступные
 * {@link RagPostProcessor} для обработки RAG-ответа.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagPostProcessingOrchestrator {

    private final List<RagPostProcessor> postProcessors;

    /**
     * Асинхронно запускает все зарегистрированные постпроцессоры.
     *
     * @param context Контекст с данными о RAG-взаимодействии.
     */
    public void process(RagProcessingContext context) {
        if (postProcessors.isEmpty()) return;
        log.info("Запуск асинхронной постобработки RAG-ответа с {} процессорами для requestId: {}", postProcessors.size(), context.requestId());
        postProcessors.forEach(processor ->
                processor.process(context)
                        .exceptionally(ex -> {
                            log.error("Ошибка в постпроцессоре '{}' для requestId {}: {}",
                                    processor.getClass().getSimpleName(), context.requestId(), ex.getMessage(), ex);
                            return null;
                        })
        );
    }
}
