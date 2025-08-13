package com.example.ragollama;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс приложения, точка входа для Spring Boot микросервиса.
 * <p>
 * Этот класс инициализирует и запускает Spring Application Context.
 * Аннотация {@code @SpringBootApplication} объединяет в себе:
 * <ul>
 *   <li>{@code @Configuration}: помечает класс как источник определений бинов.</li>
 *   <li>{@code @EnableAutoConfiguration}: пытается автоматически сконфигурировать Spring приложение
 *       на основе зависимостей в classpath.</li>
 *   <li>{@code @ComponentScan}: сканирует компоненты, конфигурации и сервисы в пакете
 *       {@code com.example.ragollama} и его подпакетах.</li>
 * </ul>
 */
@SpringBootApplication
public class RagOllamaApplication {

    /**
     * Основной метод, который запускает Spring Boot приложение.
     *
     * @param args аргументы командной строки.
     */
    public static void main(String[] args) {
        SpringApplication.run(RagOllamaApplication.class, args);
    }
}
