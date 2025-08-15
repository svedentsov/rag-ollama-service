package com.example.ragollama;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Главный класс приложения Spring Boot.
 * <p>
 * {@link SpringBootApplication} - составная аннотация, включающая:
 * <ul>
 *   <li>{@code @Configuration}: помечает класс как источник определений бинов.</li>
 *   <li>{@code @EnableAutoConfiguration}: включает автоконфигурацию Spring Boot.</li>
 *   <li>{@code @ComponentScan}: сканирует компоненты в текущем пакете и подпакетах.</li>
 * </ul>
 * {@link EnableCaching} - включает механизм кэширования Spring.
 * <p>
 * {@link EnableAsync} - включает поддержку асинхронного выполнения методов, отмеченных аннотацией {@code @Async}.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class RagOllamaApplication {

    /**
     * Точка входа в приложение.
     *
     * @param args Аргументы командной строки.
     */
    public static void main(String[] args) {
        SpringApplication.run(RagOllamaApplication.class, args);
    }
}
