package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.security.domain.FuzzingTestGeneratorAgent;
import com.example.ragollama.agent.security.domain.PersonaGeneratorAgent;
import com.example.ragollama.agent.security.domain.RbacExtractorAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для генерации Fuzzing-тестов для проверки RBAC.
 * <p>
 * Этот конвейер демонстрирует сложную последовательную цепочку:
 * <ol>
 *     <li>{@link RbacExtractorAgent} извлекает правила.</li>
 *     <li>{@link PersonaGeneratorAgent} создает атакующие персоны на основе правил.</li>
 *     <li>{@link FuzzingTestGeneratorAgent} генерирует код теста, используя персоны и правила.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class RbacFuzzingPipeline implements AgentPipeline {

    private final RbacExtractorAgent rbacExtractor;
    private final PersonaGeneratorAgent personaGenerator;
    private final FuzzingTestGeneratorAgent fuzzingTestGenerator;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "rbac-fuzzing-pipeline";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                List.of(rbacExtractor),
                List.of(personaGenerator),
                List.of(fuzzingTestGenerator)
        );
    }
}
