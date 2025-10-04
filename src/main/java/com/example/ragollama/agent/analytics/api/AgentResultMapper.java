package com.example.ragollama.agent.analytics.api;

import com.example.ragollama.agent.AgentResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Компонент-маппер для преобразования внутренних {@link AgentResult}
 * в публичные DTO {@link AgentExecutionResponse}.
 */
@Component
public class AgentResultMapper {

    /**
     * Преобразует список внутренних результатов в список публичных DTO.
     *
     * @param results Список {@link AgentResult}.
     * @return Список {@link AgentExecutionResponse}.
     */
    public List<AgentExecutionResponse> toDto(List<AgentResult> results) {
        return results.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Преобразует один внутренний результат в публичный DTO.
     *
     * @param result Один {@link AgentResult}.
     * @return Один {@link AgentExecutionResponse}.
     */
    public AgentExecutionResponse toDto(AgentResult result) {
        return AgentExecutionResponse.builder()
                .agentName(result.agentName())
                .status(result.status())
                .summary(result.summary())
                .details(result.details())
                .build();
    }
}
