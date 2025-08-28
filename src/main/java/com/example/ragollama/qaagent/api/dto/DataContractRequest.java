package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * DTO для запроса на проверку контракта данных.
 *
 * @param oldRef      Исходная Git-ссылка.
 * @param newRef      Конечная Git-ссылка.
 * @param dtoFilePath Путь к файлу с DTO для анализа.
 */
@Schema(description = "DTO для запроса на проверку контракта данных DTO")
public record DataContractRequest(
        @Schema(description = "Исходная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "main")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String oldRef,

        @Schema(description = "Конечная Git-ссылка", requiredMode = Schema.RequiredMode.REQUIRED, example = "feature/new-dto")
        @NotBlank @Pattern(regexp = "^[\\w\\-./]+$")
        String newRef,

        @Schema(description = "Путь к файлу с DTO для анализа", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "src/main/java/com/example/ragollama/chat/api/dto/ChatRequest.java")
        @NotBlank
        String dtoFilePath
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "oldRef", this.oldRef,
                "newRef", this.newRef,
                "filePath", this.dtoFilePath
        ));
    }
}
