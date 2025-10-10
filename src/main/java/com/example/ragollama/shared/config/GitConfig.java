package com.example.ragollama.shared.config;

import com.example.ragollama.agent.config.GitProperties;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для создания и настройки бинов, связанных с Git.
 * <p>
 * Централизует логику создания компонентов для взаимодействия с Git-репозиториями,
 * следуя принципу единственной ответственности.
 */
@Configuration
public class GitConfig {

    /**
     * Создает и предоставляет бин {@link UsernamePasswordCredentialsProvider} для всего приложения.
     * <p>
     * Этот бин инкапсулирует учетные данные (персональный токен доступа) для
     * аутентификации при выполнении Git-операций, таких как clone или fetch.
     * Он использует типобезопасную конфигурацию {@link GitProperties} для получения токена.
     *
     * @param gitProperties Конфигурация с URL репозитория и токеном доступа.
     * @return Сконфигурированный провайдер учетных данных.
     */
    @Bean
    public UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider(GitProperties gitProperties) {
        // Для аутентификации по токену в JGit имя пользователя может быть любым,
        // но часто используют "token" или "oauth2accesstoken". Паролем является сам токен.
        return new UsernamePasswordCredentialsProvider(gitProperties.personalAccessToken(), "");
    }
}
