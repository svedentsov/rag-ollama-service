package com.example.ragollama.rag.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO, представляющий один шаг в процессе формирования или трансформации
 * пользовательского запроса в RAG-конвейере.
 * <p>
 * Обеспечивает прозрачность и наблюдаемость этапа Query Processing.
 *
 * @param stepName    Имя агента или шага, выполнившего трансформацию (например, "HyDEAgent").
 * @param description Человекочитаемое описание выполненного действия.
 * @param result      Результат работы шага. Может быть строкой или списком строк.
 */
@Schema(description = "Один шаг в процессе формирования RAG-запроса")
public record QueryFormationStep(
        String stepName,
        String description,
        Object result // Может быть String или List<String>
) {
}
