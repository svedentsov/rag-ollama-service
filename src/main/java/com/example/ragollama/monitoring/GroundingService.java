package com.example.ragollama.monitoring;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.metrics.MetricService;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Асинхронный сервис для проверки "обоснованности" (grounding) сгенерированных ответов.
 * <p>
 * Использует LLM в роли "аудитора" для верификации того, что ответ
 * строго основан на предоставленном контексте, и не содержит выдуманных фактов (галлюцинаций).
 * Результаты проверки используются для сбора метрик и логирования.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroundingService {

    private final LlmClient llmClient;
    private final MetricService metricService;
    private final PromptService promptService;

    /**
     * Асинхронно выполняет проверку ответа на "обоснованность".
     *
     * @param context Текстовый контекст, который был использован для генерации ответа.
     * @param answer  Финальный ответ, сгенерированный RAG-системой.
     */
    @Async("applicationTaskExecutor")
    public void verify(String context, String answer) {
        if (context == null || context.isBlank()) {
            metricService.recordGroundingResult(false);
            return;
        }
        String promptString = promptService.render("groundingPrompt", Map.of("context", context, "answer", answer));
        llmClient.callChat(new Prompt(promptString), ModelCapability.FASTEST)
                .subscribe(
                        tuple -> {
                            String response = tuple.getT1();
                            boolean isGrounded = response.trim().equalsIgnoreCase("GROUNDED");
                            metricService.recordGroundingResult(isGrounded);
                            if (!isGrounded) {
                                log.warn("Обнаружен потенциально необоснованный ответ (галлюцинация).");
                            }
                        },
                        ex -> log.error("Ошибка при проверке обоснованности ответа.", ex)
                );
    }
}
