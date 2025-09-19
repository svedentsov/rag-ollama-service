package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.compliance.domain.GeoLegalNavigatorAgent;
import com.example.ragollama.agent.compliance.domain.PrivacyComplianceAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для проверки на соответствие политикам конфиденциальности.
 * <p>
 * Эта версия расширена новым агентом {@link GeoLegalNavigatorAgent}, который
 * выполняется параллельно с общей проверкой приватности.
 */
@Component
@RequiredArgsConstructor
public class PrivacyComplianceCheckPipeline implements AgentPipeline {

    private final GitInspectorAgent gitInspector;
    private final PrivacyComplianceAgent privacyComplianceAgent;
    private final GeoLegalNavigatorAgent geoLegalNavigatorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "privacy-compliance-check-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет два этапа:
     * <ol>
     *     <li>Сбор измененных файлов.</li>
     *     <li>Параллельный запуск двух анализаторов: общей политики и гео-специфичной.</li>
     * </ol>
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(gitInspector),
                List.of(privacyComplianceAgent, geoLegalNavigatorAgent)
        );
    }
}