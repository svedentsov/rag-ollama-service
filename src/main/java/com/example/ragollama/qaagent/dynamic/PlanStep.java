package com.example.ragollama.qaagent.dynamic;

import java.util.Map;

/**
 * DTO, представляющий один шаг в плане выполнения.
 *
 * @param agentName Имя агента (`QaAgent`), который должен быть выполнен на этом шаге.
 * @param arguments Карта с аргументами, которые необходимо передать в контекст агента.
 */
public record PlanStep(
        String agentName,
        Map<String, Object> arguments
) {
}
