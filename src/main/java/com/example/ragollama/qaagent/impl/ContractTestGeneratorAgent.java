package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.tools.OpenApiParser;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент, который генерирует API-тест на основе OpenAPI спецификации.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractTestGeneratorAgent implements ToolAgent {

    public static final String OPENAPI_CONTENT_KEY = "openApiContent";
    public static final String ENDPOINT_NAME_KEY = "endpointName";

    private final OpenApiParser openApiParser;
    private final LlmClient llmClient;

    private static final PromptTemplate PROMPT_TEMPLATE = new PromptTemplate("""
            ТЫ — ЭКСПЕРТНЫЙ QA-АВТОМАТИЗАТОР.
            Твоя задача — сгенерировать высококачественный, готовый к использованию API-тест на Java с использованием RestAssured.
            
            ПРАВИЛА:
            1. Используй предоставленную спецификацию эндпоинта.
            2. Сгенерируй позитивный сценарий (happy path), проверяющий успешный ответ (например, статус 200 или 202).
            3. Если в ответе есть тело, добавь базовые проверки для ключевых полей с помощью Hamcrest matchers.
            4. Твой ответ должен содержать ТОЛЬКО Java код. Без комментариев, объяснений или markdown-разметки.
            
            СПЕЦИФИКАЦИЯ ЭНДПОИНТА:
            {endpoint_details}
            """);

    @Override
    public String getName() {
        return "contract-test-generator";
    }

    @Override
    public String getDescription() {
        return "Генерирует Java/RestAssured API-тест на основе OpenAPI спецификации.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey(OPENAPI_CONTENT_KEY)
                && context.payload().containsKey(ENDPOINT_NAME_KEY);
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String openApiContent = (String) context.payload().get(OPENAPI_CONTENT_KEY);
        String endpointName = (String) context.payload().get(ENDPOINT_NAME_KEY);
        // Шаг 1: Извлекаем детали эндпоинта с помощью парсера
        String endpointDetails = openApiParser.extractEndpointDetails(openApiContent, endpointName);
        // Шаг 2: Формируем промпт для LLM
        String promptString = PROMPT_TEMPLATE.render(Map.of("endpoint_details", endpointDetails));
        // Шаг 3: Вызываем LLM для генерации кода
        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .thenApply(generatedCode -> {
                    log.info("Успешно сгенерирован API-тест для эндпоинта '{}'", endpointName);
                    String summary = "API-тест для '" + endpointName + "' успешно сгенерирован.";
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            summary,
                            Map.of(
                                    "generatedTestCode", generatedCode,
                                    "language", "java",
                                    "framework", "RestAssured"
                            )
                    );
                });
    }
}
