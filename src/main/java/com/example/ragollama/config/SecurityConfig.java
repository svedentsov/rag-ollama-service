package com.example.ragollama.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * Конфигурация безопасности Spring Security.
 * <p>
 * Этот класс настраивает правила доступа к HTTP-эндпоинтам,
 * управляет сессиями и защитой от CSRF.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Определяет цепочку фильтров безопасности для HTTP-запросов.
     * <p>
     * В данной конфигурации:
     * <ul>
     *   <li>Отключается защита от CSRF, так как мы создаем stateless REST API,
     *       где такая защита менее актуальна.</li>
     *   <li>Разрешаются все входящие запросы ({@code permitAll()}) для упрощения
     *       демонстрации. В реальном приложении здесь будут настроены правила
     *       аутентификации и авторизации.</li>
     *   <li>Устанавливается политика управления сессиями в {@code STATELESS},
     *       чтобы Spring Security не создавал HTTP-сессии.</li>
     * </ul>
     *
     * @param http объект {@link HttpSecurity} для конфигурации.
     * @return сконфигурированная цепочка фильтров {@link SecurityFilterChain}.
     * @throws Exception если при конфигурации возникает ошибка.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        // Разрешаем доступ ко всем эндпоинтам для демонстрационных целей
                        .requestMatchers("/**").permitAll()
                        // В реальном приложении здесь будут более строгие правила:
                        // .requestMatchers("/api/public/**").permitAll()
                        // .requestMatchers("/api/private/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS));
        return http.build();
    }
}
