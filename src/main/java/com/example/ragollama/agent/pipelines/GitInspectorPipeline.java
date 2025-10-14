package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.git.domain.GitInspectorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для агента, инспектирующего Git-репозиторий.
 * <p>
 * Этот класс является простой "оберткой", которая представляет
 * одиночный {@link GitInspectorAgent} как полноценный, именованный
 * конвейер в системе. Это позволяет вызывать его из контроллеров
 * унифицированным образом через {@link com.example.ragollama.agent.AgentOrchestratorService}.
 */
@Component
@RequiredArgsConstructor
public class GitInspectorPipeline implements AgentPipeline {

    private final GitInspectorAgent gitInspectorAgent;

    /**
     * {@inheritDoc}
     *
     * @return Уникальное имя конвейера "git-inspector-pipeline".
     */
    @Override
    public String getName() {
        return "git-inspector-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом {@link GitInspectorAgent}.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(gitInspectorAgent)
        );
    }
}
