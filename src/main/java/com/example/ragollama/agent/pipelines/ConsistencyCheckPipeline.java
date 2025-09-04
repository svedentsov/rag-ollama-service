package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.optimization.ConsistencyCheckerAgent;
import com.example.ragollama.optimization.CrossValidatorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер, реализующий полный цикл проверки консистентности утверждения.
 *
 * <p>Этот конвейер является примером композиции двух атомарных агентов:
 * <ol>
 *   <li>{@link ConsistencyCheckerAgent} собирает "доказательства".</li>
 *   <li>{@link CrossValidatorAgent} анализирует их и выносит вердикт.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class ConsistencyCheckPipeline implements AgentPipeline {

    private final ConsistencyCheckerAgent consistencyCheckerAgent;
    private final CrossValidatorAgent crossValidatorAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "consistency-check-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Определяет два последовательных этапа, так как валидация зависит
     * от результатов сбора доказательств.
     *
     * @return Список этапов конвейера.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(consistencyCheckerAgent),
                List.of(crossValidatorAgent)
        );
    }
}
