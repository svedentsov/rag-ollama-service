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
 * <p> Этот класс является единой точкой для сборки всех компонентов, составляющих
 * наш интеллектуальный и отказоустойчивый LLM-шлюз.
 */
@Configuration
public class AiConfig {

    /**
     * Переопределяет стандартный бин OllamaApi, чтобы он использовал наш WebClient с правильными таймаутами.
     * <p> Автоконфигурация Spring AI автоматически обнаружит этот {@code @Primary} бин и использует его
     * при создании {@link OllamaChatModel}.
     *
     * @param webClientBuilder Строитель WebClient'а, настроенный в {@link AppConfig}.
     * @param ollamaBaseUrl    URL-адрес Ollama из конфигурационного файла.
     * @return Сконфигурированный экземпляр {@link OllamaApi}.
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

    /**
     * Предоставляет основной бин {@link ChatClient} для всего приложения.
     *
     * @param ollamaChatModel Модель чата, автоматически сконфигурированная Spring AI.
     * @return Готовый к использованию {@link ChatClient}.
     */
    @Bean
    @Primary
    public ChatClient chatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

    /**
     * Создает и предоставляет наш кастомный фасад {@link LlmClient}.
     * <p> Этот метод является примером чистого Dependency Injection, собирая
     * все необходимые компоненты (шлюз, роутер, исполнитель, сервисы квот и трекинга)
     * в единый, высокоуровневый клиент.
     *
     * @param llmGateway              Низкоуровневый шлюз для прямых вызовов.
     * @param llmRouterService        Сервис для интеллектуального выбора модели.
     * @param resilientExecutor       Декоратор для обеспечения отказоустойчивости.
     * @param quotaService            Сервис для проверки квот.
     * @param usageTracker            Сервис для логирования использования.
     * @param tokenizationService     Сервис для работы с токенами.
     * @param applicationTaskExecutor Пул потоков для выполнения асинхронных операций.
     * @return Полностью сконфигурированный экземпляр {@link LlmClient}.
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
