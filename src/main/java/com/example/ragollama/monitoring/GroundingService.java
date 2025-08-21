package com.example.ragollama.monitoring;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.metrics.MetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для проверки "обоснованности" (grounding) ответов LLM.
 * Асинхронно проверяет, основан ли сгенерированный ответ на предоставленном контексте.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroundingService {

    private final LlmClient llmClient;
    private final MetricService metricService;

    private static final PromptTemplate GROUNDING_PROMPT = new PromptTemplate("""
            Твоя задача - быть строгим ревьюером. Проверь, следует ли ОТВЕТ строго из фактов,
            представленных в КОНТЕКСТЕ. Не оценивай стиль или полноту, только фактическое соответствие.
            Ответь ТОЛЬКО одним словом: ДА или НЕТ.
            
            КОНТЕКСТ:
            {context}
            
            ОТВЕТ:
            {answer}
            """);

    /**
     * Асинхронно проверяет ответ и обновляет метрики.
     *
     * @param context Контекст, предоставленный LLM.
     * @param answer  Ответ, сгенерированный LLM.
     */
    @Async("applicationTaskExecutor")
    public void verify(String context, String answer) {
        if (context == null || context.isBlank()) {
            metricService.recordGroundingResult(false); // Ответ без контекста не может быть обоснованным
            return;
        }

        String promptString = GROUNDING_PROMPT.render(Map.of("context", context, "answer", answer));
        CompletableFuture<String> verificationFuture = llmClient.callChat(new Prompt(promptString));

        verificationFuture.thenAccept(response -> {
            boolean isGrounded = response.trim().equalsIgnoreCase("ДА");
            metricService.recordGroundingResult(isGrounded);
            if (!isGrounded) {
                log.warn("Обнаружен потенциально необоснованный ответ (галлюцинация).");
            }
        }).exceptionally(ex -> {
            log.error("Ошибка при проверке обоснованности ответа.", ex);
            return null;
        });
    }
}
