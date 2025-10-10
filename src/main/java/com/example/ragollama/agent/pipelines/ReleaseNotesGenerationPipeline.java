package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.git.domain.ReleaseNotesWriterAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для генерации заметок о выпуске (release notes).
 * <p>
 * Этот класс является эталонным примером "Фасада" или "Стратегии" в
 * архитектуре агентов. Он предоставляет единую, стабильную точку входа
 * (`release-notes-generation-pipeline`) для бизнес-возможности, скрывая
 * детали ее реализации. В данный момент реализация состоит из одного шага,
 * но может быть легко расширена в будущем без изменения вызывающего кода.
 */
@Component
@RequiredArgsConstructor
public class ReleaseNotesGenerationPipeline implements AgentPipeline {

    private final ReleaseNotesWriterAgent releaseNotesWriterAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "release-notes-generation-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(releaseNotesWriterAgent)
        );
    }
}
