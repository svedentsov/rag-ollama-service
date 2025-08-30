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
        @NotBlank String alertName,
        @NotBlank String details,
        String logSnippet
) {
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "alertName", this.alertName,
                "details", this.details,
                "applicationLogs", this.logSnippet != null ? this.logSnippet : ""
        ));
    }
}
