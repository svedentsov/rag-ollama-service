package com.example.ragollama.rag.pipeline;

import reactor.core.publisher.Mono;

/**
 * Определяет контракт для одного шага в RAG-конвейере.
 * <p>
 * Реализует паттерн "Цепочка обязанностей" в асинхронном стиле. Каждый шаг
 * принимает текущее состояние конвейера в виде {@link RagFlowContext},
 * выполняет свою атомарную операцию (например, извлечение, ранжирование) и
 * возвращает {@link Mono} с обновленным состоянием для следующего шага.
 * <p>
 * Реализации этого интерфейса должны быть аннотированы {@code @Order}, чтобы
 * определить их последовательность выполнения в {@link RagPipelineOrchestrator}.
 */
@FunctionalInterface
public interface RagPipelineStep {

    /**
     * Асинхронно выполняет один шаг RAG-конвейера.
     *
     * @param context Текущий контекст выполнения, содержащий все промежуточные данные.
     * @return {@link Mono}, который по завершении будет содержать обновленный контекст.
     */
    Mono<RagFlowContext> process(RagFlowContext context);
}
