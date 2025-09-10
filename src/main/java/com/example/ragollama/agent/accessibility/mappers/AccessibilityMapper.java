package com.example.ragollama.agent.accessibility.mappers;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.accessibility.api.dto.AccessibilityAuditResponse;
import com.example.ragollama.agent.accessibility.domain.AccessibilityAuditorAgent;
import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Компонент-маппер, отвечающий за преобразование внутреннего результата
 * работы конвейера ({@link AgentResult}) в публичный DTO ответа ({@link AccessibilityAuditResponse}).
 * <p>
 * Изоляция этой логики в отдельном классе соответствует Принципу единственной
 * ответственности и упрощает тестирование контроллера.
 *
 * <p>Эта реализация использует отказоустойчивый подход: она ищет необходимый
 * {@link AccessibilityReport} во всех результатах конвейера, а не только
 * в последнем. Поиск ведется с конца списка для оптимизации, так как ожидается,
 * что релевантный результат будет ближе к концу выполнения. Это делает маппер
 * нечувствительным к добавлению в конвейер новых агентов (например, для логирования).
 */
@Component
@Slf4j
public class AccessibilityMapper {

    /**
     * Преобразует список результатов работы конвейера в DTO ответа для API.
     *
     * <p>Логика основана на строгом контракте: интересующий нас результат
     * ({@link AccessibilityReport}) должен находиться в деталях одного из
     * выполненных агентов под ключом {@link AccessibilityAuditorAgent#ACCESSIBILITY_REPORT_KEY}.
     *
     * @param agentResults Список результатов, возвращенный конвейером. Может быть null.
     * @return DTO ответа {@link AccessibilityAuditResponse} для API.
     * @throws IllegalStateException если ни один из результатов агентов не содержит
     *                               ожидаемый {@link AccessibilityReport}, что указывает
     *                               на нарушение контракта или ошибку в конфигурации конвейера.
     */
    public AccessibilityAuditResponse toResponseDto(List<AgentResult> agentResults) {
        log.debug("Маппинг {} результатов конвейера в AccessibilityAuditResponse DTO.",
                agentResults != null ? agentResults.size() : 0);
        return Optional.ofNullable(agentResults)
                .orElse(Collections.emptyList())
                .reversed()
                .stream()
                .filter(Objects::nonNull)
                .map(AgentResult::details)
                .filter(Objects::nonNull)
                .map(details -> details.get(AccessibilityAuditorAgent.ACCESSIBILITY_REPORT_KEY))
                .filter(AccessibilityReport.class::isInstance)
                .map(AccessibilityReport.class::cast)
                .findFirst() // Находим первый попавшийся, идя с конца
                .map(AccessibilityAuditResponse::new)
                .orElseThrow(() -> {
                    log.error("Нарушение контракта: в результатах конвейера не найден AccessibilityReport по ключу '{}'",
                            AccessibilityAuditorAgent.ACCESSIBILITY_REPORT_KEY);
                    return new IllegalStateException("Внутренняя ошибка: результат конвейера не содержит ожидаемый отчет.");
                });
    }
}
