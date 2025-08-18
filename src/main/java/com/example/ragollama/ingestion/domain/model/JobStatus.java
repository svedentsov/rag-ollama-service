package com.example.ragollama.ingestion.domain.model;

public enum JobStatus {
    PENDING,    // Задача ожидает в очереди
    PROCESSING, // Задача в процессе выполнения
    COMPLETED,  // Задача успешно завершена
    FAILED      // Задача завершилась с ошибкой
}
