package com.example.ragollama.agent.buganalysis.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * DTO для запроса на структурирование и обобщение баг-репорта.
 *
 * @param rawReportText Неструктурированный, "сырой" текст, описывающий проблему.
 */
@Schema(description = "DTO для запроса на структурирование баг-репорта")
public record BugReportSummaryRequest(
        @Schema(description = "Неструктурированный текст баг-репорта", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "короче я кликаю на кнопку сохранить а она не работает и ничего не происходит. должно было сохраниться.")
        @NotBlank(message = "Текст отчета не может быть пустым")
        @Size(min = 10, max = 4096, message = "Длина текста должна быть от 10 до 4096 символов")
        String rawReportText
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер агентов.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of("rawReportText", rawReportText));
    }
}
