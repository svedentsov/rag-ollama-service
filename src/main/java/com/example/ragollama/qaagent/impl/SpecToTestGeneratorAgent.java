package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.tools.OpenApiSpecParser;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.prompts.PromptService;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который генерирует исполняемый код API-тестов
 * на основе OpenAPI спецификации.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpecToTestGeneratorAgent implements QaAgent {

    private final OpenApiSpecParser specParser;
    private final LlmClient llmClient;
    private final PromptService promptService;

    @Override
    public String getName() {
        return "spec-to-test-generator";
    }

    @Override
    public String getDescription() {
        return "Генерирует Java/RestAssured/JUnit5 код для API-теста на основе OpenAPI спецификации.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return (context.payload().containsKey("specUrl") || context.payload().containsKey("specContent"))
               && context.payload().containsKey("targetEndpoint");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        OpenAPI openAPI = getOpenApi(context);
        String targetEndpoint = (String) context.payload().get("targetEndpoint");
        log.info("SpecToTestGeneratorAgent: запуск генерации теста для эндпоинта '{}'", targetEndpoint);

        // Шаг 1: Находим и форматируем детали нужного эндпоинта
        Optional<String> endpointDetails = specParser.formatOperationDetails(openAPI, targetEndpoint);

        if (endpointDetails.isEmpty()) {
            return CompletableFuture.completedFuture(new AgentResult(
                    getName(),
                    AgentResult.Status.FAILURE,
                    "Эндпоинт '" + targetEndpoint + "' не найден в предоставленной спецификации.",
                    Map.of()
            ));
        }

        // Шаг 2: Генерируем промпт и вызываем LLM
        String promptString = promptService.render("specToTest", Map.of("endpointDetails", endpointDetails.get()));
        return llmClient.callChat(new Prompt(promptString))
                .thenApply(generatedCode -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Код теста для эндпоинта '" + targetEndpoint + "' успешно сгенерирован.",
                        Map.of("generatedTestCode", generatedCode, "language", "java")
                ));
    }

    private OpenAPI getOpenApi(AgentContext context) {
        String specUrl = (String) context.payload().get("specUrl");
        return (specUrl != null)
                ? specParser.parseFromUrl(specUrl)
                : specParser.parseFromContent((String) context.payload().get("specContent"));
    }
}
