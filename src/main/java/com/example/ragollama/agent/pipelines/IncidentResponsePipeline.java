package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import com.example.ragollama.agent.incidentresponse.domain.IncidentCommanderAgent;
import com.example.ragollama.agent.incidentresponse.domain.IncidentSummarizerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для мета-агента "AI On-Call Engineer".
 * <p>
 * Эта версия расширена третьим этапом, который запускает нового агента
 * {@link IncidentCommanderAgent} для планирования и выполнения действий
 * по сдерживанию инцидента.
 */
@Component
@RequiredArgsConstructor
class IncidentResponsePipeline implements AgentPipeline {
    private final GitInspectorAgent gitInspector;
    private final IncidentSummarizerAgent incidentSummarizer;
    private final IncidentCommanderAgent incidentCommander;

    @Override
    public String getName() {
        return "incident-response-pipeline";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Определяет три последовательных этапа:
     * <ol>
     *     <li>Получение информации о недавних изменениях.</li>
     *     <li>Анализ и синтез отчета об инциденте.</li>
     *     <li>Планирование и запуск ответных действий.</li>
     * </ol>
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(gitInspector),
                List.of(incidentSummarizer),
                List.of(incidentCommander)
        );
    }
}