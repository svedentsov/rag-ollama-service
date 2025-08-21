package com.example.ragollama.agent.domain;

import com.example.ragollama.agent.api.dto.CodeGenerationRequest;
import com.example.ragollama.agent.api.dto.CodeGenerationResponse;
import com.example.ragollama.shared.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис, реализующий логику "Code Agent".
 * Использует LLM для генерации кода на основе инструкций и контекста.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenerationService {

    private final LlmClient llmClient;

    private static final PromptTemplate CODE_GEN_PROMPT_TEMPLATE = new PromptTemplate("""
            ТЫ — ЭКСПЕРТНЫЙ QA-АВТОМАТИЗАТОР. Твоя задача — сгенерировать высококачественный,
            готовый к использованию код для API-теста на Java с использованием RestAssured.
            
            ПРАВИЛА:
            1.  Используй предоставленный КОНТЕКСТ для понимания структуры API.
            2.  Следуй ИНСТРУКЦИИ, чтобы понять, что именно нужно протестировать.
            3.  Твой ответ должен содержать ТОЛЬКО Java код.
            4.  НЕ добавляй никаких объяснений, комментариев или вводных фраз.
            
            КОНТЕКСТ:
            {context}
            
            ИНСТРУКЦИЯ:
            {instruction}
            """);

    /**
     * Асинхронно генерирует код на основе запроса.
     *
     * @param request DTO с инструкцией и контекстом.
     * @return {@link CompletableFuture} с результатом генерации.
     */
    public CompletableFuture<CodeGenerationResponse> generateCode(CodeGenerationRequest request) {
        log.info("CodeGenerationAgent: получен запрос на генерацию кода.");
        String promptString = CODE_GEN_PROMPT_TEMPLATE.render(Map.of(
                "instruction", request.instruction(),
                "context", request.context()
        ));
        Prompt prompt = new Prompt(promptString);

        return llmClient.callChat(prompt)
                .thenApply(generatedCode -> new CodeGenerationResponse(generatedCode, "java"));
    }
}
