package com.example.ragollama.agent.autonomy;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
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
 * конкретные задачи, а **планирует и оркеструет запуск других конвейеров**.
 * Он декомпозирует сложную бизнес-цель в последовательность вызовов
 * предопределенных, надежных {@link com.example.ragollama.agent.AgentPipeline}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SdlcOrchestratorAgent {

    private final AgentOrchestratorService orchestratorService;
    private final PipelineRegistryService pipelineRegistry;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * DTO для представления одного шага в стратегическом плане,
     * который генерирует LLM.
     *
     * @param pipeline Имя конвейера для запуска.
     * @param context  Дополнительный контекст для этого шага.
     */
    private record StrategicPlanStep(String pipeline, Map<String, Object> context) implements Serializable {
    }

    /**
     * Принимает высокоуровневую задачу, генерирует стратегический план
     * и выполняет его, оркеструя другие аналитические конвейеры.
     *
     * @param highLevelGoal  Задача на естественном языке от пользователя.
     * @param initialContext Начальный контекст с данными (например, Git-ссылки).
     * @return {@link Mono} со списком результатов всех выполненных конвейеров.
     */
    public Mono<List<AgentResult>> execute(String highLevelGoal, AgentContext initialContext) {
        String capabilities = pipelineRegistry.getCapabilitiesCatalog();

        String promptString = promptService.render("sdlcStrategyPrompt", Map.of(
                "goal", highLevelGoal,
                "pipelines", capabilities,
                "context", initialContext.payload()
        ));

        // Шаг 1: LLM создает высокоуровневый план
        return Mono.fromFuture(() -> llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                .map(this::parseStrategicPlan)
                .flatMap(plan -> {
                    if (plan.isEmpty()) {
                        log.warn("SDLC Orchestrator: LLM-планировщик не смог составить план для цели: '{}'", highLevelGoal);
                        return Mono.just(List.of(new AgentResult("sdlc-orchestrator", AgentResult.Status.FAILURE, "Не удалось составить план для выполнения задачи.", Map.of())));
                    }

                    // Шаг 2: Последовательно выполняем каждый шаг плана, накапливая контекст
                    // Используем Flux.reduce для управления состоянием (контекст + результаты)
                    Tuple2<AgentContext, List<AgentResult>> initialState = Tuples.of(initialContext, new ArrayList<>());

                    return Flux.fromIterable(plan)
                            .reduce(initialState, (stateTuple, step) ->
                                    executePipelineStep(step, stateTuple.getT1())
                                            .map(results -> {
                                                // Обновляем состояние для следующего шага
                                                AgentContext currentContext = stateTuple.getT1();
                                                List<AgentResult> accumulatedResults = stateTuple.getT2();
                                                accumulatedResults.addAll(results);
                                                // Мержим результаты последнего шага в контекст для следующего
                                                Map<String, Object> newPayload = new HashMap<>(currentContext.payload());
                                                results.getLast().details().forEach(newPayload::putIfAbsent);
                                                AgentContext newContext = new AgentContext(newPayload);
                                                return Tuples.of(newContext, accumulatedResults);
                                            })
                                            .block() // Блокируем, т.к. reduce ожидает синхронного возврата
                            )
                            .map(Tuple2::getT2); // Возвращаем только список результатов
                });
    }

    /**
     * Выполняет один шаг плана (один конвейер) и возвращает его результат.
     *
     * @param step           Шаг плана.
     * @param currentContext Текущий накопленный контекст.
     * @return {@link Mono} с результатом выполнения конвейера.
     */
    private Mono<List<AgentResult>> executePipelineStep(StrategicPlanStep step, AgentContext currentContext) {
        log.info("SDLC Orchestrator: запуск конвейера '{}'", step.pipeline());
        // Объединяем накопленный контекст с контекстом из шага плана
        Map<String, Object> finalPayload = new HashMap<>(currentContext.payload());
        if (step.context() != null) {
            finalPayload.putAll(step.context());
        }
        AgentContext finalContext = new AgentContext(finalPayload);

        return Mono.fromFuture(() -> orchestratorService.invokePipeline(step.pipeline(), finalContext));
    }


    /**
     * Безопасно парсит JSON-ответ от LLM в список шагов плана.
     *
     * @param llmResponse Сырой ответ от LLM.
     * @return Список шагов {@link StrategicPlanStep}.
     * @throws ProcessingException если парсинг JSON не удался.
     */
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
