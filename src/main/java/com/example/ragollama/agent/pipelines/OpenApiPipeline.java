package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.openapi.domain.OpenApiAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для выполнения "RAG на лету" по OpenAPI спецификации.
 */
@Component
@RequiredArgsConstructor
public class OpenApiPipeline implements AgentPipeline {

    private final OpenApiAgent openApiAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "openapi-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Конвейер состоит из одного шага, который инкапсулирует всю
     * сложную логику RAG "на лету".
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(openApiAgent)
        );
    }
}
