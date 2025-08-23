package com.example.ragollama.agent.domain;

import com.example.ragollama.agent.api.dto.CodeGenerationRequest;
import com.example.ragollama.agent.api.dto.CodeGenerationResponse;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenerationService {

    private final LlmClient llmClient;
    private final PromptService promptService;

    public CompletableFuture<CodeGenerationResponse> generateCode(CodeGenerationRequest request) {
        log.info("CodeGenerationAgent: получен запрос на генерацию кода.");
        String promptString = promptService.render("codeGeneration", Map.of(
                "instruction", request.instruction(),
                "context", request.context()
        ));
        Prompt prompt = new Prompt(promptString);

        return llmClient.callChat(prompt)
                .thenApply(generatedCode -> new CodeGenerationResponse(generatedCode, "java"));
    }
}
