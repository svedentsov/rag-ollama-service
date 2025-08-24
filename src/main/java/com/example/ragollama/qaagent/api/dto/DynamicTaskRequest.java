package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.Optional;

/**
 * DTO для универсального запроса на выполнение динамической задачи.
 *
 * @param taskDescription Описание задачи на естественном языке.
 * @param initialContext  Опциональная карта с начальными данными для конвейера.
 */
@Schema(description = "DTO для запроса на выполнение динамической задачи")
public record DynamicTaskRequest(
        @Schema(description = "Описание задачи на естественном языке", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Проанализируй изменения между main и feature/new-auth и найди пробелы в тестовом покрытии.")
        @NotBlank @Size(max = 2048)
        String taskDescription,

        @Schema(description = "Опциональная карта с начальными данными, например, {'oldRef': 'main', 'newRef': 'feature/new-auth'}")
        Map<String, Object> initialContext
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Optional.ofNullable(initialContext).orElse(Map.of()));
    }
}
