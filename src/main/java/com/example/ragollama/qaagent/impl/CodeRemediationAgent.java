package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.CodePatch;
import com.example.ragollama.qaagent.tools.GitApiClient;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который генерирует исправления для обнаруженных проблем в коде.
 * <p>
 * Выступает в роли "парного программиста", предлагая конкретные
 * рефакторинги для улучшения качества, производительности или безопасности кода.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeRemediationAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "code-remediation-agent";
    }

    @Override
    public String getDescription() {
        return "Генерирует предложения по исправлению для обнаруженных проблем в коде.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        // Ожидает "досье" на проблему: файл, описание и опционально фрагмент кода
        return context.payload().containsKey("filePath") &&
                context.payload().containsKey("problemDescription");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String filePath = (String) context.payload().get("filePath");
        String ref = (String) context.payload().getOrDefault("ref", "main"); // По умолчанию берем из main
        String problemDescription = (String) context.payload().get("problemDescription");
        String codeSnippet = (String) context.payload().get("codeSnippet"); // Может быть null

        // Получаем полный контент файла для предоставления LLM максимального контекста
        return gitApiClient.getFileContent(filePath, ref)
                .flatMap(fullCode -> {
                    String promptString = promptService.render("codeRemediation", Map.of(
                            "fullCode", fullCode,
                            "problemDescription", problemDescription,
                            "problemSnippet", codeSnippet != null ? codeSnippet : "N/A"
                    ));
                    return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED));
                })
                .map(this::parseLlmResponse)
                .map(patch -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Предложение по исправлению кода успешно сгенерировано.",
                        Map.of("codePatch", patch)
                ))
                .toFuture();
    }

    private CodePatch parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, CodePatch.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("LLM вернула невалидный JSON для CodePatch.", e);
        }
    }
}
