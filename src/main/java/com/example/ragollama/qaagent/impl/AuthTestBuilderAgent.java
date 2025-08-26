package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.EndpointInfo;
import com.example.ragollama.qaagent.model.GeneratedTestFile;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * QA-агент, который генерирует код тестов безопасности (авторизации)
 * на основе извлеченных правил RBAC/ACL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTestBuilderAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private static final String INSUFFICIENT_PRINCIPAL = "ROLE_USER";

    @Override
    public String getName() {
        return "auth-test-builder";
    }

    @Override
    public String getDescription() {
        return "Генерирует Java/RestAssured тесты для проверки извлеченных правил авторизации.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("extractedRules");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<Map<String, String>> rules = (List<Map<String, String>>) context.payload().get("extractedRules");

        Map<EndpointInfo, List<String>> endpointsToPrincipals = rules.stream()
                .filter(rule -> !"PUBLIC".equals(rule.get("principal")))
                .collect(Collectors.groupingBy(
                        rule -> new EndpointInfo(rule.get("resource"), HttpMethod.valueOf(rule.get("action"))),
                        Collectors.mapping(rule -> rule.get("principal"), Collectors.toList())
                ));

        if (endpointsToPrincipals.isEmpty()) {
            return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Не найдено защищенных эндпоинтов для генерации тестов.", Map.of()));
        }

        return Flux.fromIterable(endpointsToPrincipals.entrySet())
                .flatMap(entry -> generateTestForEndpoint(entry.getKey(), entry.getValue()))
                .collectList()
                .map(generatedTests -> {
                    String summary = String.format("Генерация тестов безопасности завершена. Создано %d тестовых файлов.", generatedTests.size());
                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("generatedAuthTests", generatedTests));
                })
                .toFuture();
    }

    private Mono<GeneratedTestFile> generateTestForEndpoint(EndpointInfo endpoint, List<String> requiredPrincipals) {
        String requiredPrincipal = requiredPrincipals.getFirst();
        String testClassName = generateTestClassName(endpoint);
        String fileName = testClassName + ".java";

        String promptString = promptService.render("authTestBuilder", Map.of(
                "testClassName", testClassName,
                "endpointPath", endpoint.path(),
                "httpMethod", endpoint.method().name(),
                "requiredPrincipal", requiredPrincipal,
                "insufficientPrincipal", INSUFFICIENT_PRINCIPAL
        ));

        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                .map(code -> new GeneratedTestFile(fileName, code));
    }

    private String generateTestClassName(EndpointInfo endpoint) {
        String path = endpoint.path().replaceAll("[^a-zA-Z0-9]", " ").trim();
        String[] parts = path.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString() + "AuthTest";
    }
}