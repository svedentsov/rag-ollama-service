package com.example.ragollama.shared.task;

/**
 * Перечисление, определяющее жизненный цикл асинхронной задачи.
 */
public enum TaskStatus {
    /**
     * Задача в данный момент выполняется.
     */
    RUNNING,
    /**
     * Задача была успешно завершена.
     */
    COMPLETED,
    /**
     * Задача была отменена по запросу пользователя.
     */
    CANCELLED,
    /**
     * В ходе выполнения задачи произошла ошибка.
     */
    FAILED,
    CANCELLATION_REQUESTED
}
