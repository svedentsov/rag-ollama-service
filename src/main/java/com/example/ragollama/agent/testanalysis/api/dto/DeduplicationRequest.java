package com.example.ragollama.agent.testanalysis.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * DTO для запроса на поиск дубликатов для конкретного тест-кейса.
 *
 * @param testCaseId      Уникальный идентификатор тест-кейса (например, путь к файлу).
 *                        Используется для исключения самого себя из результатов поиска.
 * @param testCaseContent Полный текст тест-кейса, для которого ищутся дубликаты.
 */
@Schema(description = "DTO для запроса на поиск дубликатов тест-кейса")
public record DeduplicationRequest(
        @Schema(description = "Уникальный ID исходного тест-кейса (например, путь к файлу)", requiredMode = Schema.RequiredMode.REQUIRED, example = "src/test/java/com/example/UserServiceTest.java")
        @NotBlank
        String testCaseId,

        @Schema(description = "Полный текст тест-кейса для анализа", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 50, message = "Текст тест-кейса слишком короткий для анализа")
        String testCaseContent
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер агентов.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "testCaseId", this.testCaseId,
                "testCaseContent", this.testCaseContent
        ));
    }
}
