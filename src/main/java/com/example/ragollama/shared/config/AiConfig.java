package com.example.ragollama.shared.config;

import com.example.ragollama.agent.finops.domain.LlmUsageTracker;
import com.example.ragollama.agent.finops.domain.QuotaService;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.LlmGateway;
import com.example.ragollama.shared.llm.LlmRouterService;
import com.example.ragollama.shared.llm.ResilientLlmExecutor;
import com.example.ragollama.shared.tokenization.TokenizationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Конфигурационный класс для централизованного создания и настройки бинов, связанных с AI.
 */
@Configuration
public class AiConfig {

    /**
     * Создает и предоставляет основной бин {@link ChatClient}.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * Создает и предоставляет единственный, отказоустойчивый и интеллектуальный бин {@link LlmClient}.
     */
    @Bean
    public LlmClient llmClient(
            LlmGateway llmGateway,
            LlmRouterService llmRouterService,
            ResilientLlmExecutor resilientExecutor,
            QuotaService quotaService,
            LlmUsageTracker usageTracker,
            TokenizationService tokenizationService,
            AsyncTaskExecutor applicationTaskExecutor
    ) {
        return new LlmClient(
                llmGateway,
                llmRouterService,
                resilientExecutor,
                quotaService,
                usageTracker,
                tokenizationService,
                applicationTaskExecutor
        );
    }
}
