package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.evaluation.domain.FeedbackToTestAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конвейер для преобразования пользовательского фидбэка в регрессионный тест.
 */
@Component
@RequiredArgsConstructor
public class FeedbackToTestPipeline implements AgentPipeline {

    private final FeedbackToTestAgent feedbackToTestAgent;

    @Override
    public String getName() {
        return "feedback-to-test-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список, содержащий один этап с одним агентом.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(feedbackToTestAgent)
        );
    }
}
