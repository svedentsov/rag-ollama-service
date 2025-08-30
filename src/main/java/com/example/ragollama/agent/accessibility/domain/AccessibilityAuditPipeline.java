package com.example.ragollama.agent.accessibility.domain;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для проведения аудита доступности (a11y).
 * <p>
 * Этот класс является самодостаточной "Стратегией", которая определяет
 * последовательность агентов для выполнения задачи.
 */
@Component
@RequiredArgsConstructor
public class AccessibilityAuditPipeline implements AgentPipeline {

    private final AccessibilityAuditorAgent accessibilityAuditorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "accessibility-audit-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<QaAgent> getAgents() {
        return List.of(accessibilityAuditorAgent);
    }
}
