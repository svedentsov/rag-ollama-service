package com.example.ragollama.monitoring;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.metrics.MetricService;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroundingService {

    private final LlmClient llmClient;
    private final MetricService metricService;
    private final PromptService promptService;

    @Async("applicationTaskExecutor")
    public void verify(String context, String answer) {
        if (context == null || context.isBlank()) {
            metricService.recordGroundingResult(false);
            return;
        }

        String promptString = promptService.render("grounding", Map.of("context", context, "answer", answer));
        CompletableFuture<String> verificationFuture = llmClient.callChat(new Prompt(promptString));

        verificationFuture.thenAccept(response -> {
            boolean isGrounded = response.trim().equalsIgnoreCase("GROUNDED");
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
