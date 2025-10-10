package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.openapi.tools.OpenApiParser;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Агент, который генерирует API-тест на основе OpenAPI спецификации.
 * <p>
 * Реализует гибридный подход: сначала детерминированно парсит спецификацию
 * для извлечения точных деталей эндпоинта, а затем использует LLM для
 * генерации кода теста на основе этих деталей.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractTestGeneratorAgent implements ToolAgent {

    private final OpenApiParser openApiParser;
    private final LlmClient llmClient;
    private final PromptService promptService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "contract-test-generator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Генерирует Java/RestAssured API-тест на основе OpenAPI спецификации.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return (context.payload().containsKey("openApiContent") || context.payload().containsKey("specUrl"))
                && context.payload().containsKey("endpointName");
    }

    /**
     * Асинхронно выполняет генерацию теста.
     *
     * @param context Контекст, содержащий источник спецификации и имя целевого эндпоинта.
     * @return {@link Mono} с результатом, содержащим сгенерированный код.
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String openApiContent = (String) context.payload().get("openApiContent");
        String specUrl = (String) context.payload().get("specUrl");
        String endpointName = (String) context.payload().get("endpointName");

        String endpointDetails = (specUrl != null)
                ? openApiParser.extractEndpointDetails(openApiParser.parseFromUrl(specUrl), endpointName)
                : openApiParser.extractEndpointDetails(openApiParser.parseFromContent(openApiContent), endpointName);

        if (endpointDetails.startsWith("Детали для эндпоинта")) {
            return Mono.just(new AgentResult(getName(), AgentResult.Status.FAILURE, endpointDetails, Map.of()));
        }

        String promptString = promptService.render("contractTestGeneratorPrompt", Map.of("endpoint_details", endpointDetails));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .map(generatedCode -> {
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
