package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.tools.GitApiClient;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который генерирует код для негативных API-тестов (DAST-like).
 * <p>
 * Анализирует измененные контроллеры и создает тесты, которые пытаются
 * эксплуатировать потенциальные уязвимости (невалидный ввод, обход авторизации).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DastTestGeneratorAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;

    @Override
    public String getName() {
        return "dast-test-generator";
    }

    @Override
    public String getDescription() {
        return "Генерирует код негативных API-тестов для измененных контроллеров.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles") && context.payload().containsKey("newRef");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<String> changedFiles = (List<String>) context.payload().get("changedFiles");
        String newRef = (String) context.payload().get("newRef");

        return Flux.fromIterable(changedFiles)
                .filter(file -> file.endsWith("Controller.java"))
                .flatMap(file -> gitApiClient.getFileContent(file, newRef)
                        .flatMap(content -> generateNegativeTestsForController(file, content))
                )
                .collectList()
                .map(generatedTests -> {
                    String summary = "Генерация DAST-тестов завершена. Создано предложений: " + generatedTests.size();
                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("generatedDastTests", generatedTests));
                })
                .toFuture();
    }

    private Mono<String> generateNegativeTestsForController(String filePath, String code) {
        String promptString = promptService.render("dastTestGenerator", Map.of("filePath", filePath, "code", code));
        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED));
    }
}
