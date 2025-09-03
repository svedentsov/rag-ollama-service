package com.example.ragollama.shared.config;

import com.example.ragollama.agent.finops.domain.LlmUsageTracker;
import com.example.ragollama.agent.finops.domain.QuotaService;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.LlmGateway;
import com.example.ragollama.shared.llm.LlmRouterService;
import com.example.ragollama.shared.llm.ResilientLlmExecutor;
import com.example.ragollama.shared.tokenization.TokenizationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
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
 * <p>
 * Эта версия включает переопределение бина {@link OllamaApi}, чтобы заставить
 * Spring AI использовать наш глобально сконфигурированный {@link WebClient.Builder},
 * что решает проблему с таймаутами при долгих запросах к LLM.
 */
@Configuration
public class AiConfig {

    /**
     * Создает и предоставляет основной бин {@link ChatClient}.
     *
     * @param builder Стандартный строитель ChatClient от Spring AI.
     * @return Сконфигурированный экземпляр ChatClient.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * Создает и предоставляет единственный, отказоустойчивый и интеллектуальный бин {@link LlmClient}.
     * <p>
     * Этот бин является основным фасадом для всех взаимодействий с LLM в приложении,
     * инкапсулируя логику маршрутизации, отказоустойчивости, квотирования и трекинга.
     *
     * @param llmGateway              Низкоуровневый шлюз для вызовов LLM.
     * @param llmRouterService        Сервис для выбора оптимальной модели.
     * @param resilientExecutor       Декоратор для применения политик Resilience4j.
     * @param quotaService            Сервис для проверки квот.
     * @param usageTracker            Сервис для асинхронного логирования использования.
     * @param tokenizationService     Сервис для работы с токенами.
     * @param applicationTaskExecutor Основной пул потоков приложения.
     * @return Полностью сконфигурированный и готовый к использованию LlmClient.
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

    /**
     * Переопределяет стандартный бин {@link OllamaApi} из автоконфигурации Spring AI.
     * <p>
     * Это ключевое исправление, которое заставляет Spring AI использовать наш
     * кастомный {@code WebClient.Builder} с правильно настроенными таймаутами
     * вместо своего внутреннего `RestClient` с таймаутами по умолчанию.
     * Аннотация {@code @Primary} гарантирует, что именно этот бин будет использоваться
     * при создании {@code OllamaChatModel}.
     *
     * @param webClientBuilder Наш глобальный, настроенный WebClient.Builder.
     * @param ollamaBaseUrl    URL Ollama из application.yml.
     * @return Новый экземпляр {@link OllamaApi}, использующий наш WebClient.
     */
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
}
