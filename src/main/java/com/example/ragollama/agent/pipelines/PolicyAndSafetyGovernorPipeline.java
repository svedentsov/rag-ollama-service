package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.architecture.domain.ArchConsistencyMapperAgent;
import com.example.ragollama.agent.compliance.domain.PrivacyComplianceAgent;
import com.example.ragollama.agent.compliance.domain.ScaComplianceAgent;
import com.example.ragollama.agent.executive.domain.PolicyGuardianAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import com.example.ragollama.agent.security.domain.SastAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для "Губернатора Политик и Безопасности".
 * <p>
 * Оркестрирует полный, многовекторный аудит изменений в коде.
 */
@Component
@RequiredArgsConstructor
public class PolicyAndSafetyGovernorPipeline implements AgentPipeline {

    // Шаг 1: Сбор контекста
    private final GitInspectorAgent gitInspector;

    // Шаг 2: Параллельные проверки
    private final SastAgent sastAgent;
    private final ArchConsistencyMapperAgent archConsistencyMapper;
    private final PrivacyComplianceAgent privacyComplianceAgent;
    private final ScaComplianceAgent scaComplianceAgent;

    // Шаг 3: Синтез и вердикт
    private final PolicyGuardianAgent policyGuardianAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "policy-and-safety-governor-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<QaAgent> getAgents() {
        // ПРИМЕЧАНИЕ: Наш текущий AgentOrchestrator выполняет агентов последовательно.
        // Для истинного параллелизма потребовался бы более сложный оркестратор (как WorkflowExecutionService).
        // Но даже при последовательном выполнении, этот конвейер решает основную бизнес-задачу.
        return List.of(
                gitInspector,
                sastAgent,
                archConsistencyMapper,
                privacyComplianceAgent,
                scaComplianceAgent,
                policyGuardianAgent
        );
    }
}
