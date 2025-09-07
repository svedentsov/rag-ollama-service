package com.example.ragollama.agent.openapi.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.openapi.tools.OpenApiSpecParser;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
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
public class SpecToTestGeneratorAgent implements ToolAgent {

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

        Optional<String> endpointDetails = specParser.formatOperationDetails(openAPI, targetEndpoint);

        if (endpointDetails.isEmpty()) {
            return CompletableFuture.completedFuture(new AgentResult(
                    getName(),
                    AgentResult.Status.FAILURE,
                    "Эндпоинт '" + targetEndpoint + "' не найден в предоставленной спецификации.",
                    Map.of()
            ));
        }

        String promptString = promptService.render("specToTestPrompt", Map.of("endpointDetails", endpointDetails.get()));
        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
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
