package com.example.ragollama.optimization.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * DTO для представления одного узла в графе рабочего процесса (Workflow DAG).
 * <p>
 * Эта структура позволяет описывать сложные, нелинейные зависимости между задачами.
 *
 * @param id           Уникальный идентификатор узла в рамках одного плана.
 * @param agentName    Имя агента-инструмента для выполнения.
 * @param arguments    Аргументы для этого агента.
 * @param dependencies Список ID узлов, которые должны успешно завершиться
 *                     перед запуском этого узла.
 */
@Schema(description = "Один узел в графе рабочего процесса (DAG)")
public record WorkflowNode(
        String id,
        String agentName,
        Map<String, Object> arguments,
        List<String> dependencies
) implements Serializable {
}