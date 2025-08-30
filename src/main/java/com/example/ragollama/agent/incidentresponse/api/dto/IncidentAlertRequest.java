package com.example.ragollama.agent.incidentresponse.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DTO для входящего веб-хука от системы мониторинга.
 *
 * @param alertName  Имя сработавшего алерта.
 * @param details    Описание алерта.
 * @param logSnippet Фрагмент логов, приложенный к алерту.
 */
@Schema(description = "DTO для веб-хука от системы мониторинга")
public record IncidentAlertRequest(
        @Schema(description = "Имя сработавшего алерта", requiredMode = Schema.RequiredMode.REQUIRED, example = "HighLatencyError")
        @NotBlank
        String alertName,

        @Schema(description = "Детали алерта", requiredMode = Schema.RequiredMode.REQUIRED, example = "P99 latency for /api/v1/payments is over 2000ms")
        @NotBlank
        String details,

        @Schema(description = "Фрагмент логов, связанный с алертом")
        String logSnippet
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        // Устанавливаем разумные значения по умолчанию для Git-ссылок,
        // так как агент будет искать последние изменения в main.
        return new AgentContext(Map.of(
                "alertName", this.alertName,
                "details", this.details,
                "applicationLogs", this.logSnippet != null ? this.logSnippet : "",
                "oldRef", "main~10", // последние 10 коммитов в main
                "newRef", "main"
        ));
    }
}
