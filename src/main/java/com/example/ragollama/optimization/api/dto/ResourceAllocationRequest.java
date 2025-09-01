package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

@Schema(description = "DTO для запроса на анализ и оптимизацию ресурсов")
public record ResourceAllocationRequest(
        @Schema(description = "Имя сервиса/деплоймента для анализа", requiredMode = Schema.RequiredMode.REQUIRED, example = "rag-ollama-service")
        @NotBlank
        String serviceName,

        @Schema(description = "Текущая конфигурация ресурсов в формате YAML", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "resources:\n  requests:\n    cpu: '500m'\n    memory: '1Gi'\n  limits:\n    cpu: '1000m'\n    memory: '2Gi'")
        @NotBlank @Length(min = 20)
        String currentConfigYaml
) {
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "serviceName", this.serviceName,
                "currentConfigYaml", this.currentConfigYaml
        ));
    }
}
