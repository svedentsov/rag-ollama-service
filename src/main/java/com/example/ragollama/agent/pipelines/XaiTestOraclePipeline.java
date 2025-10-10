package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.testanalysis.domain.XaiTestOracleAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для агента "Тестовый Оракул".
 * <p>
 * Конвейер состоит из одного шага, который делегирует всю работу
 * {@link XaiTestOracleAgent} для анализа покрытия требований тестами.
 */
@Component
@RequiredArgsConstructor
public class XaiTestOraclePipeline implements AgentPipeline {

    private final XaiTestOracleAgent oracleAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "xai-test-oracle-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(List.of(oracleAgent));
    }
}
