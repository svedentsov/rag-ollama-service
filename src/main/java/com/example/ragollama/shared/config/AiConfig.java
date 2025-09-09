package com.example.ragollama.shared.config;

import com.example.ragollama.agent.finops.domain.LlmUsageTracker;
import com.example.ragollama.agent.finops.domain.QuotaService;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.LlmGateway;
import com.example.ragollama.shared.llm.LlmRouterService;
import com.example.ragollama.shared.llm.ResilientLlmExecutor;
import com.example.ragollama.shared.tokenization.TokenizationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурационный класс для централизованного создания и настройки бинов, связанных с AI.
 */
@Configuration
public class AiConfig {

    @Bean
    @Primary
    public OllamaApi ollamaApi(WebClient.Builder webClientBuilder, @Value("${spring.ai.ollama.base-url}") String ollamaBaseUrl) {
        return OllamaApi.builder()
                .baseUrl(ollamaBaseUrl)
                .restClientBuilder(RestClient.builder())
                .webClientBuilder(webClientBuilder)
                .responseErrorHandler(new DefaultResponseErrorHandler())
                .build();
    }

    @Bean
    @Primary
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi, @Value("${spring.ai.ollama.chat.options.model}") String defaultModel) {
        var defaultOptions = OllamaOptions.builder()
                .model(defaultModel)
                .temperature(0.7)
                .build();
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(defaultOptions)
                .build();
    }

    @Bean
    @Primary
    public ChatClient chatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

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
