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

    @Override
    public List<QaAgent> getAgents() {
        return List.of(feedbackToTestAgent);
    }
}
