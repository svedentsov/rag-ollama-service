package com.example.ragollama.agent.knowledgegraph.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса к Агрегатору Знаний.
 *
 * @param question Вопрос к графу знаний на естественном языке.
 */
@Schema(description = "DTO для запроса к Агрегатору Знаний")
public record KnowledgeGraphRequest(
        @Schema(description = "Вопрос к графу знаний на естественном языке", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Какие тесты проверяют требование JIRA-123?")
        @NotBlank @Length(min = 10)
        String question
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("question", this.question));
    }
}
