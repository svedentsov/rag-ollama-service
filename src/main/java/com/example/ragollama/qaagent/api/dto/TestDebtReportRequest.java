package com.example.ragollama.qaagent.api.dto;

import com.example.ragollama.qaagent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTO для запроса на генерацию отчета о тестовом техническом долге.
 * На данный момент не содержит параметров, но может быть расширен в будущем.
 */
@Schema(description = "DTO для запроса на отчет о тестовом техдолге")
public record TestDebtReportRequest(
        // В будущем здесь могут быть параметры, например, фильтрация по модулю проекта
) {
    /**
     * Преобразует DTO в {@link AgentContext}.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        // Передаем стандартный период анализа в 30 дней
        return new AgentContext(Map.of("days", 30));
    }
}
