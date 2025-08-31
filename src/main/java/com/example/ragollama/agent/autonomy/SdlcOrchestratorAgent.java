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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI-супервизор ("AI-Директор"), управляющий всем жизненным циклом разработки по
 * высокоуровневым командам на естественном языке.
 * <p>
 * Является высшим уровнем в иерархии агентов, который не выполняет
 * конкретные задачи, а **планирует и оркестрирует запуск других конвейеров**.
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

        String promptString = promptService.render("sdlcStrategy", Map.of(
                "goal", highLevelGoal,
                "pipelines", capabilities,
                "context", initialContext.payload()
        ));

        // Шаг 1: LLM создает высокоуровневый план
        return Mono.fromFuture(() -> llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                .map(this::parseStrategicPlan)
                .flatMapMany(Flux::fromIterable)
                // Шаг 2: Выполняем каждый шаг плана (каждый конвейер) последовательно
                .concatMap(step -> {
                    log.info("SDLC Orchestrator: запуск конвейера '{}'", step.pipeline);
                    // Объединяем начальный контекст с контекстом из плана
                    Map<String, Object> finalContext = new HashMap<>(initialContext.payload());
                    if (step.context != null) {
                        finalContext.putAll(step.context);
                    }
                    return Mono.fromFuture(() -> orchestratorService.invokePipeline(step.pipeline, new AgentContext(finalContext)));
                })
                .flatMap(Flux::fromIterable) // "Расплющиваем" List<AgentResult> в поток AgentResult
                .collectList();
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
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить стратегический план от LLM: {}", llmResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON-план.", e);
        }
    }
}
