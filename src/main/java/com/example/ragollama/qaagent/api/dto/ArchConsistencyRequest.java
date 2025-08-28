package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.Length;

import java.util.List;
import java.util.Map;

/**
 * DTO для запроса на проверку архитектурной консистентности.
 *
 * @param architecturePrinciples Описание эталонной архитектуры на естественном языке.
 * @param changedFiles           Список путей к измененным файлам для анализа.
 */
@Schema(description = "DTO для запроса на проверку архитектурной консистентности")
public record ArchConsistencyRequest(
        @Schema(description = "Описание эталонной архитектуры", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Мы используем трехслойную архитектуру: Controller -> Service -> Repository. Контроллеры не должны содержать бизнес-логику и не могут вызывать репозитории напрямую.")
        @NotEmpty @Length(min = 50)
        String architecturePrinciples,

        @Schema(description = "Список путей к измененным файлам для анализа", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        List<String> changedFiles
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "architecturePrinciples", this.architecturePrinciples,
                "changedFiles", this.changedFiles
        ));
    }
}
