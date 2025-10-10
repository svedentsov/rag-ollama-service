package com.example.ragollama.agent.review;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

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
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String action = (String) context.payload().getOrDefault("actionToApprove", "неизвестное действие");
        log.info("HITL Gate: Формирование запроса на утверждение для действия '{}'", action);
        return Mono.just(
                new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Ожидание утверждения для: " + action,
                        Map.of()
                )
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requiresApproval() {
        return true;
    }
}
