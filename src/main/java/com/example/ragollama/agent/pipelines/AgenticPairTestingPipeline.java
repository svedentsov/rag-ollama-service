package com.example.ragollama.agent.pipelines;

import com.example.ragollama.agent.AgentPipeline;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.testanalysis.domain.AdversarialTesterAgent;
import com.example.ragollama.agent.testanalysis.domain.TestDesignerAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Реализация конвейера для "парного тестирования" (Agentic Pair Testing).
 * <p>
 * Этот конвейер реализует продвинутый паттерн "self-play", где два AI-агента
 * работают в тандеме для создания более полного тестового покрытия:
 * <ol>
 *     <li><b>"Строитель" ({@link TestDesignerAgent}):</b> Генерирует основной
 *     позитивный тест-кейс на основе требований.</li>
 *     <li><b>"Разрушитель" ({@link AdversarialTesterAgent}):</b> Анализирует
 *     требования и тест "Строителя", чтобы найти упущенные сценарии и
 *     сгенерировать для них негативные и граничные тесты.</li>
 * </ol>
 * Такая последовательность обеспечивает более глубокое и всестороннее
 * покрытие требований по сравнению с работой одного агента.
 */
@Component
@RequiredArgsConstructor
public class AgenticPairTestingPipeline implements AgentPipeline {

    private final TestDesignerAgent testDesignerAgent;
    private final AdversarialTesterAgent adversarialTesterAgent;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "agentic-pair-testing-pipeline";
    }

    /**
     * {@inheritDoc}
     *
     * @return Список из двух последовательных этапов.
     */
    @Override
    public List<List<QaAgent>> getStages() {
        return List.of(
                // Этап 1: Создать "happy path" тест
                List.of(testDesignerAgent),
                // Этап 2: Найти пробелы и создать негативные/граничные тесты
                List.of(adversarialTesterAgent)
        );
    }
}
