package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.shared.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * QA-агент, который генерирует чек-лист для ручного тестирования
 * на основе описания функциональности.
 * <p>
 * Этот агент является примером чисто генеративной задачи, не использующей RAG.
 * Он демонстрирует, как платформа агентов может быть расширена для выполнения
 * разнообразных AI-задач.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChecklistGeneratorAgent implements QaAgent {

    public static final String FEATURE_DESCRIPTION_KEY = "featureDescription";
    private final LlmClient llmClient;

    /**
     * Промпт для генерации чек-листа.
     * Он содержит четкие инструкции и примеры (few-shot prompting)
     * для получения структурированного и качественного ответа от LLM.
     * <p>
     * В production-системе этот шаблон следует вынести во внешний .ftl файл
     * и управлять им через PromptService.
     */
    private static final PromptTemplate PROMPT_TEMPLATE = new PromptTemplate("""
            ТЫ — ОПЫТНЫЙ QA-ИНЖЕНЕР. Твоя задача — составить детальный чек-лист для ручного тестирования,
            основываясь на предоставленном ОПИСАНИИ ФУНКЦИОНАЛЬНОСТИ.
            
            ПРАВИЛА:
            1.  Создай список конкретных, проверяемых шагов (тест-кейсов).
            2.  Каждый пункт чек-листа должен начинаться с новой строки.
            3.  Не добавляй маркеры (типа `*` или `-`) или нумерацию.
            4.  Покрывай как позитивные, так и негативные сценарии (например, ввод некорректных данных).
            5.  Твой ответ должен содержать ТОЛЬКО пункты чек-листа. Без заголовков и заключений.
            
            ОПИСАНИЕ ФУНКЦИОНАЛЬНОСТИ:
            {feature_description}
            """);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "checklist-generator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Генерирует чек-лист для ручного тестирования по описанию функциональности.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey(FEATURE_DESCRIPTION_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String featureDescription = (String) context.payload().get(FEATURE_DESCRIPTION_KEY);
        String promptString = PROMPT_TEMPLATE.render(Map.of("feature_description", featureDescription));

        return llmClient.callChat(new Prompt(promptString))
                .thenApply(llmResponse -> {
                    List<String> checklistItems = parseToList(llmResponse);
                    String summary = "Чек-лист успешно сгенерирован. Найдено " + checklistItems.size() + " пунктов для проверки.";
                    log.info("ChecklistGeneratorAgent: сгенерирован чек-лист из {} пунктов.", checklistItems.size());

                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            summary,
                            Map.of("checklist", checklistItems)
                    );
                });
    }

    /**
     * Безопасно парсит многострочный ответ от LLM в список строк.
     *
     * @param llmResponse Сырой ответ от LLM.
     * @return Список очищенных от лишних пробелов строк.
     */
    private List<String> parseToList(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return List.of();
        }
        return Arrays.stream(llmResponse.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }
}
