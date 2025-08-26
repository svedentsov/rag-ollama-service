package com.example.ragollama.qaagent.autonomy;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.dynamic.DynamicPipelineExecutionService;
import com.example.ragollama.qaagent.dynamic.PlanStep;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Автономный "мета-агент", выступающий в роли AI QA Lead.
 * <p>
 * Этот агент работает по расписанию, проводит комплексный аудит
 * состояния проекта с помощью других аналитических агентов, а затем
 * использует LLM для триажа, приоритизации и создания плана
 * по устранению наиболее критичных проблем.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutonomousQALeadAgent {

    private final ProjectHealthAggregatorService healthAggregatorService;
    private final DynamicPipelineExecutionService executionService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * Запускает полный цикл автономного анализа и планирования по расписанию.
     */
    @Scheduled(cron = "${app.analysis.autonomy.cron:0 0 7 * * MON-FRI}") // По умолчанию - в 7 утра по будням
    public void runAutonomousAnalysis() {
        log.info("Запуск планового автономного анализа состояния проекта...");
        AgentContext context = new AgentContext(Map.of("days", 7)); // Анализируем данные за последнюю неделю

        // Шаг 1: Асинхронно собираем все отчеты о здоровье проекта
        healthAggregatorService.aggregateHealthReports(context)
                .thenCompose(healthReport -> {
                    if (healthReport.isEmpty()) {
                        log.info("Анализ завершен. Критичных проблем не обнаружено.");
                        return CompletableFuture.completedFuture(null);
                    }
                    // Шаг 2: Передаем отчеты LLM для стратегического анализа и планирования
                    try {
                        String reportsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(healthReport);
                        String promptString = promptService.render("qaLeadStrategy", Map.of("reportsJson", reportsJson));

                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .thenCompose(llmResponse -> {
                                    List<PlanStep> actionPlan = parseLlmResponse(llmResponse);
                                    if (actionPlan.isEmpty()) {
                                        log.info("LLM-стратег проанализировал отчеты, но не счел необходимым создавать план действий.");
                                        return CompletableFuture.completedFuture(null);
                                    }
                                    // Шаг 3: Запускаем сгенерированный план на выполнение
                                    log.warn("АВТОНОМНЫЙ АГЕНТ СГЕНЕРИРОВАЛ ПЛАН ИЗ {} ШАГОВ. ЗАПУСК НА ВЫПОЛНЕНИЕ...", actionPlan.size());
                                    return executionService.executePlan(actionPlan, new AgentContext(Map.of()), null).toFuture();
                                });

                    } catch (JsonProcessingException e) {
                        return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации отчетов для AI-стратега", e));
                    }
                })
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Автономный анализ завершился с ошибкой.", ex);
                    } else if (result != null) {
                        log.info("Автономный план успешно выполнен. Результаты: {}", result);
                    }
                });
    }

    private List<PlanStep> parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ProcessingException("LLM вернула невалидный JSON-план.", e);
        }
    }
}
