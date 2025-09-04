package com.example.ragollama.rag.postprocessing;

import java.util.concurrent.CompletableFuture;

/**
 * Функциональный интерфейс, определяющий контракт для компонента-постпроцессора.
 * <p>Каждый постпроцессор выполняет одну атомарную сквозную задачу
 * (например, логирование, верификация, сбор метрик) после того, как основной
 * RAG-конвейер сгенерировал ответ. Все операции выполняются асинхронно.
 */
@FunctionalInterface
public interface RagPostProcessor {

    /**
     * Асинхронно выполняет операцию постобработки.
     *
     * @param context Контекст, содержащий все данные о RAG-взаимодействии.
     * @return {@link CompletableFuture}, завершающийся после выполнения операции.
     */
    CompletableFuture<Void> process(RagProcessingContext context);
}
