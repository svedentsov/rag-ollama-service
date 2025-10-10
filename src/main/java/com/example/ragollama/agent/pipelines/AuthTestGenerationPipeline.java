package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.security.domain.AuthTestBuilderAgent;
import com.example.ragollama.agent.security.domain.RbacExtractorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для автоматической генерации тестов безопасности
 * на основе правил контроля доступа.
 * <p>
 * Конвейер выполняет два последовательных шага:
 * <ol>
 *     <li>{@link RbacExtractorAgent} извлекает правила контроля доступа из кода.</li>
 *     <li>{@link AuthTestBuilderAgent} использует эти правила для генерации
 *     кода позитивных и негативных API-тестов.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class AuthTestGenerationPipeline implements AgentPipeline {

    private final RbacExtractorAgent rbacExtractor;
    private final AuthTestBuilderAgent testBuilder;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "auth-test-generation-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список из двух последовательных этапов.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(rbacExtractor),
                List.of(testBuilder)
        );
    }
}
