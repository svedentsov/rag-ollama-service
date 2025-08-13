package com.example.ragollama.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter; // Импортируйте класс
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для бинов, связанных с Spring AI.
 */
@Configuration
public class AiConfig {

    /**
     * Создает и настраивает основной бин ChatClient для всего приложения.
     * <p>
     * Spring AI автоматически предоставляет бин {@link ChatClient.Builder}, который
     * уже настроен для работы с Ollama (согласно вашему application.yml).
     * Мы используем этот builder для создания конкретного экземпляра ChatClient,
     * который затем может быть внедрен в другие сервисы.
     *
     * @param builder автоматически внедряемый ChatClient.Builder.
     * @return Сконфигурированный бин {@link ChatClient}.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * Создает бин TokenTextSplitter.
     * <p>
     * Этот сплиттер используется для разбиения больших документов на чанки
     * перед их векторизацией. Создание его как бина позволяет легко внедрять
     * его в сервисы, такие как DocumentService.
     *
     * @return новый экземпляр {@link TokenTextSplitter}.
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }
}
