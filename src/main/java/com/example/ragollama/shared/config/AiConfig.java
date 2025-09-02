package com.example.ragollama.shared.config;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.LlmGateway;
import com.example.ragollama.shared.llm.LlmRouterService;
import com.example.ragollama.shared.llm.ResilientLlmExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для централизованного создания и настройки бинов, связанных с AI.
 * <p>
 * Эта финальная версия собирает декомпозированные компоненты (`LlmGateway`,
 * `ResilientLlmExecutor`, `LlmRouterService`) в единый, отказоустойчивый
 * и интеллектуальный бин {@link LlmClient}. Она также явно создает
 * бин {@link ChatClient}, необходимый для работы шлюза.
 */
@Configuration
public class AiConfig {

    /**
     * Создает и предоставляет основной бин {@link ChatClient}, который будет
     * использоваться для низкоуровневого взаимодействия с LLM.
     *
     * @param builder Автоматически сконфигурированный строитель ChatClient от Spring AI.
     * @return Готовый к использованию экземпляр {@link ChatClient}.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * Создает и предоставляет единственный, отказоустойчивый и интеллектуальный бин {@link LlmClient}.
     *
     * @param llmGateway        Низкоуровневый шлюз для взаимодействия со Spring AI.
     * @param llmRouterService  Сервис для выбора модели на основе требуемых возможностей.
     * @param resilientExecutor Декоратор, применяющий политики отказоустойчивости.
     * @return Финальный, готовый к использованию в приложении экземпляр {@link LlmClient}.
     */
    @Bean
    public LlmClient llmClient(
            LlmGateway llmGateway,
            LlmRouterService llmRouterService,
            ResilientLlmExecutor resilientExecutor
    ) {
        return new LlmClient(
                llmGateway,
                llmRouterService,
                resilientExecutor
        );
    }
}
