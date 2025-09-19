package com.example.ragollama.optimization.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.Optional;

/**
 * DTO для высокоуровневого запроса к Workflow Orchestrator.
 * <p>
 * Этот объект передает бизнес-цель пользователя и начальный набор данных,
 * необходимых для ее выполнения, в {@link com.example.ragollama.optimization.WorkflowPlannerAgent}.
 *
 * @param goal           Высокоуровневая цель на естественном языке, которую должен
 *                       достичь оркестратор.
 * @param initialContext Карта с начальными данными для выполнения цели.
 *                       Ключи и значения зависят от конкретной цели
 *                       (например, `oldRef`, `newRef` для анализа релиза).
 */
@Schema(description = "DTO для запроса на выполнение сложного рабочего процесса (workflow)")
public record WorkflowRequest(
        @Schema(description = "Высокоуровневая цель на естественном языке", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Проведи полный аудит безопасности и покрытия тестами для изменений между main и feature/auth, а затем создай отчет.")
        @NotBlank @Size(max = 2048)
        String goal,

        @Schema(description = "Начальный контекст с данными для выполнения",
                example = "{\"oldRef\": \"main\", \"newRef\": \"feature/auth\", \"jacocoReportContent\": \"<...xml...>\"}")
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