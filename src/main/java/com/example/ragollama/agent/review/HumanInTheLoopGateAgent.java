package com.example.ragollama.agent.review;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Специализированный агент-шлюз, реализующий механизм Human-in-the-Loop.
 * <p>
 * Его единственная задача — приостановить выполнение конвейера и вернуть
 * информацию о том, какое действие требует утверждения. Этот агент не выполняет
 * никакой полезной работы сам, а служит "точкой останова" в плане.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HumanInTheLoopGateAgent implements ToolAgent {

    @Override
    public String getName() {
        return "human-in-the-loop-gate-agent";
    }

    @Override
    public String getDescription() {
        return "Приостанавливает выполнение плана и ожидает утверждения от человека для следующего шага. Принимает 'actionToApprove' в качестве аргумента.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("actionToApprove");
    }

    /**
     * Этот метод не будет вызван напрямую, так как `DynamicPipelineExecutionService`
     * имеет специальную логику для обработки `requiresApproval()`.
     * Но для полноты контракта мы возвращаем фиктивный результат.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String action = (String) context.payload().getOrDefault("actionToApprove", "неизвестное действие");
        log.info("HITL Gate: Формирование запроса на утверждение для действия '{}'", action);
        return CompletableFuture.completedFuture(
                new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Ожидание утверждения для: " + action,
                        Map.of() // Детали будут добавлены самим исполнителем
                )
        );
    }

    /**
     * Главная особенность этого агента — он всегда требует утверждения.
     * `DynamicPipelineExecutionService` перехватит этот флаг и приостановит
     * выполнение конвейера *перед* вызовом метода `execute`.
     *
     * @return всегда {@code true}.
     */
    @Override
    public boolean requiresApproval() {
        return true;
    }
}
