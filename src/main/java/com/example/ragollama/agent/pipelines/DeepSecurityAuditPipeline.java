package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.security.domain.AuthRiskDetectorAgent;
import com.example.ragollama.agent.security.domain.RbacExtractorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для глубокого аудита безопасности, сфокусированного
 * на контроле доступа (RBAC).
 * <p>
 * Конвейер выполняет два последовательных шага:
 * <ol>
 *     <li>{@link RbacExtractorAgent} извлекает правила контроля доступа из кода.</li>
 *     <li>{@link AuthRiskDetectorAgent} анализирует эти правила на предмет
 *     потенциальных уязвимостей.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class DeepSecurityAuditPipeline implements AgentPipeline {

    private final RbacExtractorAgent rbacExtractor;
    private final AuthRiskDetectorAgent riskDetector;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "deep-security-audit-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список из двух последовательных этапов.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(rbacExtractor),
                List.of(riskDetector)
        );
    }
}
