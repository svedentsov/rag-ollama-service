package com.example.ragollama.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для бинов, связанных с Spring AI.
 * <p>
 * Этот класс централизует создание и настройку компонентов,
 * необходимых для работы с искусственным интеллектом, таких как
 * клиенты для чатов и инструменты для обработки текста.
 */
@Configuration
public class AiConfig {

    /**
     * Создает и настраивает основной бин {@link ChatClient} для всего приложения.
     * <p>
     * Spring AI автоматически предоставляет бин {@link ChatClient.Builder}, который
     * уже сконфигурирован для работы с Ollama (согласно `application.yml`).
     * Мы используем этот builder для создания конкретного экземпляра {@code ChatClient},
     * который затем может быть внедрен в сервисы для взаимодействия с LLM.
     *
     * @param builder Автоматически внедряемый {@link ChatClient.Builder} от Spring AI.
     * @return Сконфигурированный и готовый к использованию бин {@link ChatClient}.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * Создает бин {@link TokenTextSplitter}.
     * <p>
     * Этот сплиттер используется для разбиения больших текстовых документов на более
     * мелкие фрагменты (чанки) перед их векторизацией и сохранением в векторное хранилище.
     * Создание его в качестве бина позволяет легко внедрять и переиспользовать
     * его в различных сервисах, например, в {@code DocumentService}.
     *
     * @return Новый экземпляр {@link TokenTextSplitter} с настройками по умолчанию.
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }
}
