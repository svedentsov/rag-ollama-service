package com.example.ragollama.agent.autonomy;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.registry.ToolRegistryService;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.Serializable;
import java.util.*;

/**
 * AI-супервизор ("AI-Директор"), управляющий всем жизненным циклом разработки по
 * высокоуровневым командам на естественном языке.
 * <p>
 * Является высшим уровнем в иерархии агентов, который не выполняет
 * конкретные задачи, а **планирует и оркеструет запуск других конвейеров и агентов**.
 * Он декомпозирует сложную бизнес-цель в последовательность вызовов
 * предопределенных, надежных компонентов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SdlcOrchestratorAgent {

    private final AgentOrchestratorService orchestratorService;
    private final ToolRegistryService toolRegistry;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * DTO для представления одного шага в стратегическом плане,
     * который генерирует LLM.
     *
     * @param pipeline Имя компонента (конвейера или агента) для запуска.
     * @param context  Дополнительный контекст для этого шага.
     */
    private record StrategicPlanStep(String pipeline, Map<String, Object> context) implements Serializable {
    }

    /**
     * Принимает высокоуровневую задачу, генерирует стратегический план
     * и выполняет его, оркеструя другие компоненты.
     *
     * @param highLevelGoal  Задача на естественном языке от пользователя.
     * @param initialContext Начальный контекст с данными (например, Git-ссылки).
     * @return {@link Mono} со списком результатов всех выполненных компонентов.
     */
    public Mono<List<AgentResult>> execute(String highLevelGoal, AgentContext initialContext) {
        String capabilities = toolRegistry.getToolDescriptionsAsJson();

        String promptString = promptService.render("sdlcStrategyPrompt", Map.of(
                "goal", highLevelGoal,
                "pipelines", capabilities,
                "context", initialContext.payload()
        ));

        return Mono.fromFuture(() -> llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                .map(this::parseStrategicPlan)
                .flatMap(plan -> {
                    if (plan.isEmpty()) {
                        log.warn("SDLC Orchestrator: LLM-планировщик не смог составить план для цели: '{}'", highLevelGoal);
                        return Mono.just(List.of(new AgentResult("sdlc-orchestrator", AgentResult.Status.FAILURE, "Не удалось составить план для выполнения задачи.", Map.of())));
                    }

                    Tuple2<AgentContext, List<AgentResult>> initialState = Tuples.of(initialContext, new ArrayList<>());

                    return Flux.fromIterable(plan)
                            .reduce(initialState, (stateTuple, step) ->
                                    executeStep(step, stateTuple.getT1())
                                            .map(results -> {
                                                AgentContext currentContext = stateTuple.getT1();
                                                List<AgentResult> accumulatedResults = stateTuple.getT2();
                                                accumulatedResults.addAll(results);
                                                Map<String, Object> newPayload = new HashMap<>(currentContext.payload());
                                                if (!results.isEmpty()) {
                                                    results.getLast().details().forEach(newPayload::putIfAbsent);
                                                }
                                                AgentContext newContext = new AgentContext(newPayload);
                                                return Tuples.of(newContext, accumulatedResults);
                                            })
                                            .block()
                            )
                            .map(Tuple2::getT2);
                });
    }

    private Mono<List<AgentResult>> executeStep(StrategicPlanStep step, AgentContext currentContext) {
        log.info("SDLC Orchestrator: запуск компонента '{}'", step.pipeline());
        Map<String, Object> finalPayload = new HashMap<>(currentContext.payload());
        if (step.context() != null) {
            finalPayload.putAll(step.context());
        }
        AgentContext finalContext = new AgentContext(finalPayload);
        return Mono.fromFuture(() -> orchestratorService.invoke(step.pipeline(), finalContext));
    }


    private List<StrategicPlanStep> parseStrategicPlan(String llmResponse) {
        try {
            String json = JsonExtractorUtil.extractJsonBlock(llmResponse);
            if (json.isEmpty()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить стратегический план от LLM: {}", llmResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON-план.", e);
        }
    }
}