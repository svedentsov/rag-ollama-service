package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

/**
 * DTO для запроса на создание комплексного чек-листа.
 *
 * @param goal   Высокоуровневая цель (например, "регрессионное тестирование").
 * @param oldRef Исходная Git-ссылка для определения контекста изменений.
 * @param newRef Конечная Git-ссылка.
 */
@Schema(description = "DTO для запроса на создание комплексного чек-листа")
public record ChecklistBuilderRequest(
        @Schema(description = "Высокоуровневая цель", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Создать полный регрессионный чек-лист для следующих изменений")
        @NotBlank @Length(min = 10)
        String goal,

        @Schema(description = "Исходная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "main")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "feature/new-feature")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String newRef
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "goal", this.goal,
                "oldRef", this.oldRef,
                "newRef", this.newRef
        ));
    }
}
