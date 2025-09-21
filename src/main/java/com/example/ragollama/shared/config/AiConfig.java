package com.example.ragollama.shared.config;

import com.example.ragollama.agent.finops.domain.LlmUsageTracker;
import com.example.ragollama.agent.finops.domain.QuotaService;
import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.LlmGateway;
import com.example.ragollama.shared.llm.LlmRouterService;
import com.example.ragollama.shared.llm.ResilientLlmExecutor;
import com.example.ragollama.shared.tokenization.TokenizationService;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * Конфигурационный класс для централизованного создания и настройки бинов, связанных с AI.
 * <p>
 * Этот класс является единой точкой для сборки всех компонентов, составляющих
 * наш интеллектуальный и отказоустойчивый LLM-шлюз.
 */
@Configuration
@RequiredArgsConstructor
public class AiConfig {

    private final AppProperties appProperties;

    /**
     * Переопределяет стандартный бин OllamaApi, чтобы он использовал WebClient с увеличенными таймаутами.
     *
     * @param ollamaBaseUrl URL-адрес Ollama из конфигурационного файла.
     * @return Сконфигурированный экземпляр {@link OllamaApi}.
     */
    @Bean
    @Primary
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String ollamaBaseUrl) {
        final var ollamaHttpClientProps = appProperties.httpClient().ollama();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) appProperties.httpClient().connectTimeout().toMillis())
                .responseTimeout(ollamaHttpClientProps.responseTimeout())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(ollamaHttpClientProps.readWriteTimeout().toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(ollamaHttpClientProps.readWriteTimeout().toSeconds(), TimeUnit.SECONDS)));
        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
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
     * <p>
     * Этот метод является примером чистого Dependency Injection, собирая
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
