package com.example.ragollama.agent.accessibility.domain;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для проведения аудита доступности (a11y).
 *
 * <p>Этот конвейер состоит из одного этапа, содержащего одного агента,
 * так как в данном простом сценарии нет параллельных задач.
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
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(accessibilityAuditorAgent)
        );
    }
}
