package com.example.ragollama.shared.task;

/**
 * Перечисление, определяющее возможные статусы асинхронной задачи.
 */
public enum TaskStatus {
    /**
     * Задача находится в процессе выполнения.
     */
    RUNNING,
    /**
     * Задача успешно завершена.
     */
    COMPLETED,
    /**
     * Задача была отменена.
     */
    CANCELLED,
    /**
     * Задача завершилась с ошибкой.
     */
    FAILED,
    /**
     * Был получен запрос на отмену, но задача еще не перешла в статус CANCELLED.
     */
    CANCELLATION_REQUESTED
}
