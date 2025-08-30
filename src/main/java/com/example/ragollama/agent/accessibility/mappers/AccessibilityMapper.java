package com.example.ragollama.agent.accessibility.mappers;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.accessibility.api.dto.AccessibilityAuditResponse;
import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Компонент-маппер, отвечающий за преобразование внутреннего результата
 * работы агента {@link AgentResult} в публичный DTO ответа {@link AccessibilityAuditResponse}.
 * <p>
 * Изоляция этой логики в отдельном классе соответствует Принципу единственной
 * ответственности и упрощает тестирование контроллера.
 */
@Component
public class AccessibilityMapper {

    /**
     * Преобразует список {@link AgentResult} в {@link AccessibilityAuditResponse}.
     *
     * @param agentResults Список результатов, возвращенный конвейером.
     *                     Ожидается, что он содержит один результат от агента "accessibility-auditor".
     * @return DTO ответа для API.
     */
    public AccessibilityAuditResponse toResponseDto(List<AgentResult> agentResults) {
        // Извлекаем отчет из деталей последнего результата в конвейере
        AccessibilityReport report = agentResults.stream()
                .reduce((first, second) -> second) // Получаем последний элемент
                .flatMap(lastResult -> lastResult.details().entrySet().stream()
                        .filter(entry -> entry.getValue() instanceof AccessibilityReport)
                        .map(entry -> (AccessibilityReport) entry.getValue())
                        .findFirst())
                .orElse(new AccessibilityReport("Отчет не был сгенерирован.", List.of(), List.of()));

        return new AccessibilityAuditResponse(report);
    }
}
