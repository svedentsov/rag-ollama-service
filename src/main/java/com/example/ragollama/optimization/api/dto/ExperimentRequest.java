package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * DTO для запроса на запуск A/B-тестирования RAG-конфигураций.
 *
 * @param baselineDescription Описание цели эксперимента и базовой конфигурации.
 * @param variants            Карта вариантов для тестирования. Ключ - имя варианта,
 *                            значение - карта с переопределениями конфигурации.
 */
@Schema(description = "DTO для запуска A/B-тестирования RAG-конфигураций")
public record ExperimentRequest(
        @Schema(description = "Описание цели эксперимента", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Проверка влияния увеличения topK на метрику Recall")
        @NotEmpty
        String baselineDescription,

        @Schema(description = "Карта вариантов для тестирования. Ключ - имя варианта, значение - карта с переопределениями конфигурации.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "{\"topK_increased\": {\"app.rag.retrieval.topK\": 6}}")
        @NotEmpty
        @Size(min = 1)
        Map<String, Map<String, Object>> variants
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "baselineDescription", this.baselineDescription,
                "variants", this.variants
        ));
    }
}