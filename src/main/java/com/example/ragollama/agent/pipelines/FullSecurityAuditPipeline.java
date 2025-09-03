package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.compliance.domain.PrivacyComplianceAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import com.example.ragollama.agent.security.domain.AuthRiskDetectorAgent;
import com.example.ragollama.agent.security.domain.RbacExtractorAgent;
import com.example.ragollama.agent.security.domain.SastAgent;
import com.example.ragollama.agent.security.domain.SecurityReportAggregatorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер, реализующий полный, многовекторный аудит безопасности.
 */
@Component
@RequiredArgsConstructor
public class FullSecurityAuditPipeline implements AgentPipeline {

    private final GitInspectorAgent gitInspector;
    private final SastAgent sastAgent;
    private final RbacExtractorAgent rbacExtractor;
    private final AuthRiskDetectorAgent authRiskDetector;
    private final PrivacyComplianceAgent privacyComplianceAgent;
    private final SecurityReportAggregatorAgent aggregatorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "full-security-audit-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет четыре этапа:
     * 1. Получение измененных файлов.
     * 2. Параллельный запуск первичных анализаторов кода (SAST, RBAC, PII).
     * 3. Запуск анализатора рисков, который зависит от результатов RBAC-экстрактора.
     * 4. Финальная агрегация всех отчетов.
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(gitInspector),
                List.of(sastAgent, rbacExtractor, privacyComplianceAgent),
                List.of(authRiskDetector),
                List.of(aggregatorAgent)
        );
    }
}
