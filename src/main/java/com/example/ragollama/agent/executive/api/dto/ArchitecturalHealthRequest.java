package com.example.ragollama.agent.executive.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTO для запроса на анализ архитектурной эволюции.
 */
@Schema(description = "DTO для запроса на анализ долгосрочного здоровья архитектуры")
public record ArchitecturalHealthRequest() { // No parameters needed for this version
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Пустой контекст.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of());
    }
}
