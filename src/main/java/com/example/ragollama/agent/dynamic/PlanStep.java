package com.example.ragollama.agent.dynamic;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO (Data Transfer Object), представляющий один шаг в динамическом плане выполнения.
 * <p>
 * Этот record является неизменяемой (immutable) и сериализуемой структурой данных,
 * что позволяет безопасно хранить его в базе данных в виде JSON.
 *
 * @param agentName Имя агента (`QaAgent`), который должен быть выполнен на этом шаге.
 *                  Это имя используется {@link ToolRegistryService} для поиска
 *                  соответствующего бина агента.
 * @param arguments Карта с аргументами, которые необходимо передать в контекст агента.
 *                  Эти аргументы могут быть извлечены LLM-планировщиком из
 *                  исходного запроса пользователя или быть статически заданными.
 */
public record PlanStep(
        String agentName,
        Map<String, Object> arguments
) implements Serializable {
}
