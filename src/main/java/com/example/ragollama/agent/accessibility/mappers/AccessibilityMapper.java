package com.example.ragollama.agent.accessibility.mappers;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.accessibility.api.dto.AccessibilityAuditResponse;
import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import org.springframework.stereotype.Component;

import java.util.Collections;
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
     * Преобразует список результатов работы конвейера в DTO ответа.
     * <p>
     * Логика основана на контракте, что интересующий нас результат
     * (`AccessibilityReport`) будет находиться в деталях **последнего**
     * выполненного агента в конвейере.
     *
     * @param agentResults Список результатов, возвращенный конвейером.
     *                     Ожидается, что он не пуст.
     * @return DTO ответа {@link AccessibilityAuditResponse} для API.
     */
    public AccessibilityAuditResponse toResponseDto(List<AgentResult> agentResults) {
        if (agentResults == null || agentResults.isEmpty()) {
            return createEmptyResponse();
        }
        AgentResult lastResult = agentResults.get(agentResults.size() - 1);
        Object reportObject = lastResult.details().get("accessibilityReport");
        if (reportObject instanceof AccessibilityReport report) {
            return new AccessibilityAuditResponse(report);
        }
        return createEmptyResponse();
    }

    /**
     * Создает DTO ответа по умолчанию, если отчет не был сгенерирован.
     *
     * @return DTO с сообщением об ошибке.
     */
    private AccessibilityAuditResponse createEmptyResponse() {
        AccessibilityReport emptyReport = new AccessibilityReport(
                "Отчет не был сгенерирован из-за внутренней ошибки.",
                Collections.emptyList(),
                Collections.emptyList()
        );
        return new AccessibilityAuditResponse(emptyReport);
    }
}
