package com.example.ragollama.rag.postprocessing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис-оркестратор, который асинхронно запускает все доступные
 * {@link RagPostProcessor} для обработки RAG-ответа.
 * <p>
 * Spring автоматически внедряет список всех бинов, реализующих интерфейс
 * {@link RagPostProcessor}, упорядоченный согласно аннотации {@code @Order}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagPostProcessingOrchestrator {

    private final List<RagPostProcessor> postProcessors;
    private final AsyncTaskExecutor applicationTaskExecutor;

    /**
     * Асинхронно запускает все зарегистрированные постпроцессоры.
     * <p>
     * Метод работает по принципу "fire-and-forget": он не ожидает завершения
     * всех задач, чтобы не блокировать и не замедлять возврат ответа
     * конечному пользователю. Обработка ошибок каждого постпроцессора
     * инкапсулирована внутри его реализации.
     *
     * @param context Контекст с данными о RAG-взаимодействии.
     */
    public void process(RagProcessingContext context) {
        log.info("Запуск асинхронной постобработки RAG-ответа с {} процессорами.", postProcessors.size());
        postProcessors.stream()
                .map(processor -> processor.process(context)
                        .exceptionally(ex -> {
                            log.error("Ошибка в постпроцессоре '{}': {}", processor.getClass().getSimpleName(), ex.getMessage(), ex);
                            return null;
                        })
                )
                .forEach(future -> {
                });
    }
}
