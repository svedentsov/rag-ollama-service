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
     * Определяет цепочку фильтров безопасности для всех HTTP-запросов.
     * <p>
     * В данной конфигурации:
     * <ul>
     *   <li>Отключается защита от CSRF ({@code csrf(AbstractHttpConfigurer::disable)}),
     *       что является стандартной практикой для stateless REST API, где аутентификация
     *       происходит по токенам, а не по сессионным cookie.</li>
     *   <li>Разрешаются все входящие запросы ({@code requestMatchers("/**").permitAll()}) для
     *       упрощения демонстрации. В реальном приложении здесь будут настроены правила
     *       аутентификации и авторизации (например, с использованием JWT).</li>
     *   <li>Устанавливается политика управления сессиями в {@code STATELESS},
     *       чтобы Spring Security не создавал и не использовал HTTP-сессии.</li>
     * </ul>
     *
     * @param http объект {@link HttpSecurity} для текучей конфигурации правил безопасности.
     * @return Сконфигурированная и готовая к использованию цепочка фильтров {@link SecurityFilterChain}.
     * @throws Exception если при конфигурации возникает ошибка.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        // Для демонстрационных целей разрешаем доступ ко всем эндпоинтам.
                        // В реальном приложении здесь будут более строгие правила.
                        .requestMatchers("/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS));
        return http.build();
    }
}
