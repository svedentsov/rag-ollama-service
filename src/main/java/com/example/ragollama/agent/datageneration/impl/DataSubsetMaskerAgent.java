package com.example.ragollama.agent.datageneration.impl;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.datageneration.domain.DataSubsetService;
import com.example.ragollama.agent.datageneration.model.DataSubsetResult;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который создает безопасные, репрезентативные подмножества
 * производственных данных для использования в тестовых окружениях.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSubsetMaskerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final DataSubsetService dataSubsetService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "data-subset-masker";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Генерирует SQL для создания репрезентативного подмножества данных и маскирует в нем PII.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requiresApproval() {
        // Выполнение сгенерированного SQL - рискованная операция, требующая подтверждения.
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("tableSchema") && context.payload().containsKey("goal");
    }

    /**
     * Асинхронно выполняет создание подмножества данных.
     *
     * @param context Контекст, содержащий схему таблицы и цель.
     * @return {@link CompletableFuture} с результатом, содержащим сгенерированный SQL и замаскированные данные.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String tableSchema = (String) context.payload().get("tableSchema");
        String goal = (String) context.payload().get("goal");
        Integer limit = (Integer) context.payload().getOrDefault("limit", 100);

        // Шаг 1: Используем LLM для генерации SQL-запроса
        return generateSql(tableSchema, goal, limit)
                .thenApply(generatedSql -> {
                    // Шаг 2: Выполняем SQL и маскируем данные
                    List<Map<String, Object>> maskedData = dataSubsetService.executeAndMask(generatedSql);

                    int rowsSelected = maskedData.size();
                    List<Map<String, Object>> returnedData = maskedData.stream().limit(limit).toList();
                    int rowsReturned = returnedData.size();

                    DataSubsetResult result = new DataSubsetResult(generatedSql, rowsSelected, rowsReturned, returnedData);

                    String summary = String.format(
                            "Успешно сгенерирован SQL и создано подмножество из %d строк (%d возвращено).",
                            rowsSelected, rowsReturned
                    );

                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("subsetResult", result));
                });
    }

    /**
     * Вызывает LLM для генерации SQL-запроса на основе схемы и цели.
     */
    private CompletableFuture<String> generateSql(String schema, String goal, int limit) {
        String promptString = promptService.render("dataSubsetSqlGenerator", Map.of(
                "table_schema", schema,
                "goal", goal,
                "limit", limit * 5 // Запрашиваем с запасом, чтобы потом обрезать
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .thenApply(response -> response.replaceAll("(?i)```sql|```", "").trim());
    }
}
