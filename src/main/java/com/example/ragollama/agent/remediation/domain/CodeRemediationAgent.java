package com.example.ragollama.agent.remediation.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.git.tools.GitApiClient;
import com.example.ragollama.agent.remediation.model.CodePatch;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AI-агент, который генерирует исправления для обнаруженных проблем в коде.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeRemediationAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

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
        return context.payload().containsKey("filePath") &&
                context.payload().containsKey("problemDescription");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String filePath = (String) context.payload().get("filePath");
        String ref = (String) context.payload().getOrDefault("ref", "main");
        String problemDescription = (String) context.payload().get("problemDescription");
        String codeSnippet = (String) context.payload().get("codeSnippet");

        return gitApiClient.getFileContent(filePath, ref)
                .flatMap(fullCode -> {
                    String promptString = promptService.render("codeRemediationPrompt", Map.of(
                            "fullCode", fullCode,
                            "problemDescription", problemDescription,
                            "problemSnippet", codeSnippet != null ? codeSnippet : "N/A"
                    ));
                    Prompt prompt = new Prompt(new UserMessage(promptString));
                    return llmClient.callChat(prompt, ModelCapability.BALANCED, true);
                })
                .map(tuple -> parseLlmResponse(tuple.getT1()))
                .map(patch -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Предложение по исправлению кода успешно сгенерировано.",
                        Map.of("codePatch", patch)
                ));
    }

    private CodePatch parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, CodePatch.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("LLM вернула невалидный JSON для CodePatch.", e);
        }
    }
}
