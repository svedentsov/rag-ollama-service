package com.example.ragollama.agent;

import java.util.concurrent.CompletableFuture;

/**
 * Определяет универсальный контракт для всех QA-агентов в системе.
 * <p>
 * Добавлен метод `requiresApproval` для поддержки Human-in-the-Loop.
 */
public interface QaAgent {

    /**
     * Возвращает уникальное, машиночитаемое имя агента.
     * Используется для идентификации в логах, метриках и API.
     *
     * @return Имя агента (например, "bug-duplicate-detector").
     */
    String getName();

    /**
     * Возвращает человекочитаемое описание того, что делает агент.
     * Используется в UI и документации.
     *
     * @return Описание назначения агента.
     */
    String getDescription();

    /**
     * Проверяет, может ли данный агент обработать предоставленный контекст.
     * <p>
     * Оркестратор использует этот метод для выбора подходящего агента.
     * Например, агент для анализа логов вернет `true`, только если
     * в контексте есть поле `logContent`.
     *
     * @param context Контекст входящего запроса.
     * @return `true`, если агент может обработать этот контекст, иначе `false`.
     */
    boolean canHandle(AgentContext context);

    /**
     * Асинхронно выполняет основную логику агента.
     *
     * @param context Контекст с входными данными для агента.
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * результат работы агента в виде {@link AgentResult}.
     */
    CompletableFuture<AgentResult> execute(AgentContext context);

    /**
     * Указывает, требует ли выполнение этого агента предварительного
     * утверждения человеком.
     *
     * @return {@code true}, если агент выполняет рискованные или дорогостоящие
     * операции, иначе {@code false}.
     */
    default boolean requiresApproval() {
        return false;
    }
}
