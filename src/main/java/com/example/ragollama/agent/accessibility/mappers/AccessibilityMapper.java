package com.example.ragollama.agent.accessibility.mappers;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.accessibility.api.dto.AccessibilityAuditResponse;
import com.example.ragollama.agent.accessibility.domain.AccessibilityAuditorAgent;
import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Компонент-маппер, отвечающий за преобразование внутреннего результата
 * работы агента {@link AgentResult} в публичный DTO ответа {@link AccessibilityAuditResponse}.
 * <p>
 * Изоляция этой логики в отдельном классе соответствует Принципу единственной
 * ответственности и упрощает тестирование контроллера.
 * <p>
 * Эта версия реализует принцип "fail-fast": если ожидаемый результат не найден
 * в данных от агента, будет выброшено исключение, что предотвращает
 * "тихие" ошибки и упрощает отладку.
 */
@Component
public class AccessibilityMapper {

    /**
     * Преобразует список результатов работы конвейера в DTO ответа.
     * <p>
     * Логика основана на строгом контракте: интересующий нас результат
     * (`AccessibilityReport`) должен находиться в деталях **последнего**
     * выполненного агента в конвейере под ключом
     * {@link AccessibilityAuditorAgent#ACCESSIBILITY_REPORT_KEY}.
     *
     * @param agentResults Список результатов, возвращенный конвейером.
     *                     Ожидается, что он не пуст.
     * @return DTO ответа {@link AccessibilityAuditResponse} для API.
     * @throws IllegalStateException если результат последнего агента не содержит
     *                               ожидаемый {@link AccessibilityReport}, что указывает
     *                               на нарушение контракта или ошибку в конвейере.
     */
    public AccessibilityAuditResponse toResponseDto(List<AgentResult> agentResults) {
        return Optional.ofNullable(agentResults)
                .filter(results -> !results.isEmpty())
                .map(results -> results.get(results.size() - 1))
                .map(AgentResult::details)
                .map(details -> details.get(AccessibilityAuditorAgent.ACCESSIBILITY_REPORT_KEY))
                .filter(AccessibilityReport.class::isInstance)
                .map(AccessibilityReport.class::cast)
                .map(AccessibilityAuditResponse::new)
                .orElseThrow(() -> new IllegalStateException("Нарушение контракта: результат конвейера не содержит ожидаемый AccessibilityReport."));
    }
}
