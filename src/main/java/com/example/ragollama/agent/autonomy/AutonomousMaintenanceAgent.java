package com.example.ragollama.agent.autonomy;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.dynamic.DynamicPipelineExecutionService;
import com.example.ragollama.agent.dynamic.PlanStep;
import com.example.ragollama.agent.jira.model.JiraTicketRequest;
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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI-агент, который автономно анализирует состояние проекта, проводит триаж
 * проблем и создает план по их устранению в виде задач в Jira.
 * <p>
 * Этот агент является вершиной аналитической системы, действуя как
 * проактивный AI Team Lead, который следит за здоровьем кодовой базы.
 * Он не реализует интерфейс {@code ToolAgent}, так как является
 * высокоуровневым оркестратором, а не инструментом.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutonomousMaintenanceAgent implements QaAgent {

    private final AgentOrchestratorService orchestratorService;
    private final DynamicPipelineExecutionService executionService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "autonomous-maintenance-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Автономно анализирует состояние проекта, выявляет техдолг и создает задачи на его устранение.";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Этот агент является инициирующим, поэтому он всегда готов к работе.
     *
     * @param context Контекст выполнения (обычно пустой).
     * @return Всегда {@code true}.
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Реализует многошаговый асинхронный процесс:
     * 1. Запускает все доступные аналитические конвейеры для сбора отчетов о состоянии проекта.
     * 2. Агрегирует эти отчеты и передает их LLM для проведения триажа и приоритизации.
     * 3. LLM генерирует список задач (тикетов), которые необходимо создать.
     * 4. Агент преобразует этот список в динамический план выполнения для агента `jira-ticket-creator`.
     * 5. Запускает исполнение этого плана, которое создаст тикеты после утверждения человеком.
     *
     * @param context Контекст выполнения.
     * @return {@link CompletableFuture} с результатом, содержащим информацию о сгенерированном плане.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        log.info("Запуск автономного агента по обслуживанию...");

        // Шаг 1: Асинхронно запускаем аналитический конвейер.
        CompletableFuture<List<AgentResult>> testDebtFuture = orchestratorService.invokePipeline("test-debt-report-pipeline", context);

        // Шаг 2: Когда отчеты готовы, передаем их LLM для триажа.
        return testDebtFuture.thenCompose(healthCheckResults -> {
            if (healthCheckResults.isEmpty() || healthCheckResults.get(0).details().isEmpty()) {
                String summary = "Анализ завершен. Критичного технического долга не обнаружено.";
                log.info(summary);
                return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of()));
            }

            try {
                String reportsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(healthCheckResults);
                String promptString = promptService.render("autonomousTriagePrompt", Map.of("reportsJson", reportsJson));

                return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                        .thenCompose(llmResponse -> {
                            List<JiraTicketRequest> ticketsToCreate = parseLlmResponse(llmResponse);
                            if (ticketsToCreate.isEmpty()) {
                                return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Анализ завершен. Новых задач по техдолгу не создано.", Map.of()));
                            }

                            // Шаг 3: Преобразуем ответ LLM в план и запускаем исполнитель.
                            List<PlanStep> plan = ticketsToCreate.stream()
                                    .map(ticket -> new PlanStep("jira-ticket-creator", Map.of(
                                            "title", ticket.title(),
                                            "description", ticket.description()
                                            // В реальной системе здесь были бы и другие поля
                                    )))
                                    .collect(Collectors.toList());

                            return executionService.executePlan(plan, new AgentContext(Map.of()), null)
                                    .map(executionResults -> {
                                        String summary = "Автономный анализ завершен. Создан план по созданию " + executionResults.size() + " тикетов, ожидающих утверждения.";
                                        return new AgentResult(
                                                getName(),
                                                AgentResult.Status.SUCCESS,
                                                summary,
                                                Map.of("createdTicketsPlan", executionResults)
                                        );
                                    }).toFuture();
                        });
            } catch (JsonProcessingException e) {
                return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации отчетов для триажа", e));
            }
        });
    }

    /**
     * Безопасно парсит JSON-ответ от LLM, содержащий список тикетов для создания.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Список объектов {@link JiraTicketRequest}.
     * @throws ProcessingException если парсинг не удался.
     */
    private List<JiraTicketRequest> parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ProcessingException("LLM вернула невалидный JSON для создания тикетов.", e);
        }
    }
}
