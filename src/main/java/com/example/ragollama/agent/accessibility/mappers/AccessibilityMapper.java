package com.example.ragollama.agent.accessibility.mappers;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.accessibility.api.dto.AccessibilityAuditResponse;
import com.example.ragollama.agent.accessibility.domain.AccessibilityAuditorAgent;
import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Компонент-маппер, отвечающий за преобразование внутреннего результата
 * работы конвейера {@link AgentResult} в публичный DTO ответа {@link AccessibilityAuditResponse}.
 *
 * <p>Изоляция этой логики в отдельном классе соответствует Принципу единственной
 * ответственности и упрощает тестирование контроллера.
 *
 * <p>Эта версия реализует отказоустойчивый подход: она ищет необходимый
 * {@link AccessibilityReport} во всех результатах конвейера, а не только
 * в последнем. Это делает маппер нечувствительным к добавлению новых агентов
 * (например, для логирования) в конец конвейера.
 */
@Component
public class AccessibilityMapper {

    /**
     * Преобразует список результатов работы конвейера в DTO ответа.
     *
     * <p>Логика основана на строгом контракте: интересующий нас результат
     * (`AccessibilityReport`) должен находиться в деталях одного из
     * выполненных агентов под ключом
     * {@link AccessibilityAuditorAgent#ACCESSIBILITY_REPORT_KEY}.
     *
     * <p>Поиск ведется с конца списка для оптимизации, так как ожидается,
     * что релевантный результат будет ближе к концу выполнения конвейера.
     *
     * @param agentResults Список результатов, возвращенный конвейером.
     * @return DTO ответа {@link AccessibilityAuditResponse} для API.
     * @throws IllegalStateException если ни один из результатов агентов не содержит
     *                               ожидаемый {@link AccessibilityReport}, что указывает
     *                               на нарушение контракта или ошибку в конвейере.
     */
    public AccessibilityAuditResponse toResponseDto(List<AgentResult> agentResults) {
        return Optional.ofNullable(agentResults)
                .orElse(Collections.emptyList())
                .reversed() // Java 21 feature: creates a reversed-order view of the List
                .stream()
                .filter(Objects::nonNull)
                .map(AgentResult::details)
                .filter(Objects::nonNull)
                .map(details -> details.get(AccessibilityAuditorAgent.ACCESSIBILITY_REPORT_KEY))
                .filter(AccessibilityReport.class::isInstance)
                .map(AccessibilityReport.class::cast)
                .findFirst() // Находим первый попавшийся, идя с конца
                .map(AccessibilityAuditResponse::new)
                .orElseThrow(() -> new IllegalStateException("Нарушение контракта: результат конвейера не содержит ожидаемый AccessibilityReport."));
    }
}
