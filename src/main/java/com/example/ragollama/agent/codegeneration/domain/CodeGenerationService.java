package com.example.ragollama.agent.codegeneration.domain;

import com.example.ragollama.agent.codegeneration.api.dto.CodeGenerationRequest;
import com.example.ragollama.agent.codegeneration.api.dto.CodeGenerationResponse;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Сервис, реализующий логику агента для генерации кода.
 * <p>
 * Эта версия использует полностью асинхронный подход на базе Project Reactor,
 * возвращая {@link Mono} для неблокирующей обработки.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenerationService {

    private final LlmClient llmClient;
    private final PromptService promptService;

    /**
     * Асинхронно генерирует фрагмент кода на основе запроса.
     *
     * @param request DTO с инструкцией и контекстом для генерации.
     * @return {@link Mono}, который по завершении будет содержать
     * {@link CodeGenerationResponse} со сгенерированным кодом.
     */
    public Mono<CodeGenerationResponse> generateCode(CodeGenerationRequest request) {
        log.info("CodeGenerationAgent: получен запрос на генерацию кода.");
        String promptString = promptService.render("codeGenerationPrompt", Map.of(
                "instruction", request.instruction(),
                "context", request.context()
        ));
        Prompt prompt = new Prompt(promptString);

        return llmClient.callChat(prompt, ModelCapability.BALANCED)
                .map(tuple -> new CodeGenerationResponse(tuple.getT1(), "java", tuple.getT2().getContents()));
    }

    /**
     * Асинхронно генерирует фрагмент кода в потоковом режиме.
     *
     * @param request DTO с инструкцией и контекстом для генерации.
     * @return {@link Flux}, который по мере генерации будет передавать
     * фрагменты (токены) сгенерированного кода.
     */
    public Flux<String> generateCodeStream(CodeGenerationRequest request) {
        log.info("CodeGenerationAgent: получен потоковый запрос на генерацию кода.");
        String promptString = promptService.render("codeGenerationPrompt", Map.of(
                "instruction", request.instruction(),
                "context", request.context()
        ));
        Prompt prompt = new Prompt(promptString);
        return llmClient.streamChat(prompt, ModelCapability.BALANCED);
    }
}
