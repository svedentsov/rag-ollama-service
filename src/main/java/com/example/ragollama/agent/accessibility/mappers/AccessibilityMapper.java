package com.example.ragollama.agent.accessibility.mappers;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.accessibility.api.dto.AccessibilityAuditResponse;
import com.example.ragollama.agent.accessibility.domain.AccessibilityAuditorAgent;
import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Компонент-маппер, отвечающий за преобразование внутреннего результата
 * работы конвейера ({@link AgentResult}) в публичный DTO ответа ({@link AccessibilityAuditResponse}).
 * <p>
 * Эта версия использует более надежную и читаемую логику для извлечения
 * необходимого результата из списка.
 */
@Component
@Slf4j
public class AccessibilityMapper {

    /**
     * Преобразует список результатов работы конвейера в DTO ответа для API.
     * <p>
     * Логика основана на строгом контракте: мы ищем результат, сгенерированный
     * агентом {@link AccessibilityAuditorAgent}, и извлекаем из его деталей
     * отчет по ключу {@link AccessibilityAuditorAgent#ACCESSIBILITY_REPORT_KEY}.
     *
     * @param agentResults Список результатов, возвращенный конвейером. Может быть null или пустым.
     * @return DTO ответа {@link AccessibilityAuditResponse} для API.
     * @throws IllegalStateException если в результатах конвейера не найден
     *                               ожидаемый {@link AccessibilityReport}.
     */
    public AccessibilityAuditResponse toResponseDto(List<AgentResult> agentResults) {
        if (agentResults == null || agentResults.isEmpty()) {
            log.error("Нарушение контракта: конвейер не вернул ни одного результата.");
            throw new IllegalStateException("Внутренняя ошибка: конвейер не вернул результат.");
        }

        // Ищем результат от конкретного агента, что более надежно, чем брать последний.
        return agentResults.stream()
                .filter(result -> AccessibilityAuditorAgent.class.getSimpleName().equals(result.agentName()))
                .findFirst()
                .map(result -> {
                    log.debug("Маппинг результата от агента '{}' в DTO.", result.agentName());
                    Object reportObject = result.details().get(AccessibilityAuditorAgent.ACCESSIBILITY_REPORT_KEY);
                    if (reportObject instanceof AccessibilityReport report) {
                        return new AccessibilityAuditResponse(report);
                    }
                    return null;
                })
                .orElseThrow(() -> {
                    log.error("Нарушение контракта: в результатах конвейера не найден AccessibilityReport от агента '{}'",
                            AccessibilityAuditorAgent.class.getSimpleName());
                    return new IllegalStateException("Внутренняя ошибка: результат конвейера не содержит ожидаемый отчет.");
                });
    }
}
