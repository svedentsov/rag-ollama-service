package com.example.ragollama.shared.task;

import lombok.Data;

import java.util.concurrent.CompletableFuture;

@Data
public class TaskRecord {
    private final CompletableFuture<?> future;
    private volatile TaskStatus status;

    /**
     * Конструктор, который инициализирует задачу со статусом RUNNING.
     *
     * @param future CompletableFuture, который нужно отслеживать.
     */
    public TaskRecord(CompletableFuture<?> future) {
        this.future = future;
        this.status = TaskStatus.RUNNING;
    }
}
