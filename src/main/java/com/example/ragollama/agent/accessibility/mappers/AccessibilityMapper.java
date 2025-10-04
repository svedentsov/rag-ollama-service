package com.example.ragollama.agent.accessibility.mappers;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.accessibility.api.dto.AccessibilityAuditResponse;
import com.example.ragollama.agent.accessibility.domain.AccessibilityAuditorAgent;
import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Компонент-маппер, отвечающий за преобразование внутреннего результата
 * работы конвейера ({@link AgentResult}) в публичный DTO ответа ({@link AccessibilityAuditResponse}).
 * <p>
 * Изоляция этой логики в отдельном классе соответствует Принципу единственной
 * ответственности и упрощает тестирование контроллера.
 * <p>
 * Эта реализация извлекает результат из финального {@link AgentResult}
 * конвейера, что стало возможным благодаря паттерну "Эволюционирующий Контекст"
 * в {@link com.example.ragollama.agent.AgentOrchestratorService}.
 */
@Component
@Slf4j
public class AccessibilityMapper {

    /**
     * Преобразует список результатов работы конвейера в DTO ответа для API.
     *
     * <p>Логика основана на строгом контракте: интересующий нас результат
     * ({@link AccessibilityReport}) должен находиться в деталях <b>финального</b>
     * результата конвейера под ключом {@link AccessibilityAuditorAgent#ACCESSIBILITY_REPORT_KEY}.
     *
     * @param agentResults Список результатов, возвращенный конвейером. Может быть null или пустым.
     * @return DTO ответа {@link AccessibilityAuditResponse} для API.
     * @throws IllegalStateException если финальный результат конвейера не содержит
     *                               ожидаемый {@link AccessibilityReport}, что указывает
     *                               на нарушение контракта или ошибку в конфигурации.
     */
    public AccessibilityAuditResponse toResponseDto(List<AgentResult> agentResults) {
        if (agentResults == null || agentResults.isEmpty()) {
            log.error("Нарушение контракта: конвейер не вернул ни одного результата.");
            throw new IllegalStateException("Внутренняя ошибка: конвейер не вернул результат.");
        }

        AgentResult lastResult = agentResults.getLast();
        log.debug("Маппинг финального результата от агента '{}' в DTO.", lastResult.agentName());

        return Optional.ofNullable(lastResult.details())
                .map(details -> details.get(AccessibilityAuditorAgent.ACCESSIBILITY_REPORT_KEY))
                .filter(AccessibilityReport.class::isInstance)
                .map(AccessibilityReport.class::cast)
                .map(AccessibilityAuditResponse::new)
                .orElseThrow(() -> {
                    log.error("Нарушение контракта: в финальном результате конвейера не найден AccessibilityReport по ключу '{}'",
                            AccessibilityAuditorAgent.ACCESSIBILITY_REPORT_KEY);
                    return new IllegalStateException("Внутренняя ошибка: результат конвейера не содержит ожидаемый отчет.");
                });
    }
}
