package com.example.ragollama.agent.accessibility.mappers;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.accessibility.api.dto.AccessibilityAuditResponse;
import com.example.ragollama.agent.accessibility.domain.AccessibilityAuditorAgent;
import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Компонент-маппер, отвечающий за преобразование внутреннего результата
 * работы агента ({@link AgentResult}) в публичный DTO ответа ({@link AccessibilityAuditResponse}).
 * <p>
 * Эта версия использует более надежную и читаемую логику для извлечения
 * необходимого результата, основываясь на явном контракте с агентом
 * (константа {@link AccessibilityAuditorAgent#ACCESSIBILITY_REPORT_KEY}
 * и проверка типа с помощью {@code instanceof}).
 */
@Component
@Slf4j
public class AccessibilityMapper {

    /**
     * Преобразует результат работы агента в DTO ответа для API.
     * <p>
     * Логика основана на строгом контракте: мы извлекаем из деталей результата
     * отчет по ключу {@link AccessibilityAuditorAgent#ACCESSIBILITY_REPORT_KEY} и
     * выполняем типобезопасную проверку перед преобразованием.
     *
     * @param agentResult Результат, возвращенный конвейером.
     * @return DTO ответа {@link AccessibilityAuditResponse} для API.
     * @throws IllegalStateException если в результате конвейера не найден
     *                               ожидаемый {@link AccessibilityReport} или он имеет неверный тип.
     */
    public AccessibilityAuditResponse toResponseDto(AgentResult agentResult) {
        log.debug("Маппинг результата от агента '{}' в DTO.", agentResult.agentName());

        Object reportObject = agentResult.details().get(AccessibilityAuditorAgent.ACCESSIBILITY_REPORT_KEY);

        // Типобезопасная проверка перед приведением типа
        if (reportObject instanceof AccessibilityReport report) {
            return new AccessibilityAuditResponse(report);
        }

        // Если контракт нарушен, выбрасываем исключение с детальным сообщением
        String foundType = (reportObject != null) ? reportObject.getClass().getName() : "null";
        log.error("Нарушение контракта: результат от агента '{}' не содержит AccessibilityReport по ключу '{}'. Вместо этого найден: {}",
                agentResult.agentName(), AccessibilityAuditorAgent.ACCESSIBILITY_REPORT_KEY, foundType);
        throw new IllegalStateException("Внутренняя ошибка: результат конвейера не содержит ожидаемый отчет.");
    }
}
