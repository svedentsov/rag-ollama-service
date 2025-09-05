package com.example.ragollama.agent.architecture.model;

/**
 * DTO для представления одной связи (зависимости) в графе.
 *
 * @param sourceId ID исходного компонента.
 * @param targetId ID компонента, от которого зависит исходный.
 */
public record DependencyLink(String sourceId, String targetId) {
}
