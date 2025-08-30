package com.example.ragollama.agent.web.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент-инструмент для скрапинга (веб-кроулинга) веб-страниц.
 * <p>
 * Использует библиотеку Jsoup для извлечения чистого текста со страницы по URL.
 * Является простым, но мощным инструментом для сбора внешних данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebCrawlerAgent implements ToolAgent {

    /**
     * Ключ в AgentContext, по которому агент ищет URL для скрапинга.
     */
    public static final String URL_KEY = "competitorUrl";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "web-crawler";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Извлекает текстовое содержимое веб-страницы по ее URL.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey(URL_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            String urlString = (String) context.payload().get(URL_KEY);

            // Усиленная валидация входных данных
            if (urlString == null || urlString.isBlank()) {
                return new AgentResult(getName(), AgentResult.Status.FAILURE, "URL для скрапинга не предоставлен.", Map.of());
            }

            try {
                // Проверяем, что URL валиден, перед использованием
                new URL(urlString);

                log.info("WebCrawlerAgent: запуск скрапинга для URL: {}", urlString);
                String textContent = Jsoup.connect(urlString).get().text();
                String summary = "Скрапинг успешно завершен. Извлечено символов: " + textContent.length();
                return new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        summary,
                        Map.of("scrapedText", textContent)
                );
            } catch (MalformedURLException e) {
                log.error("Предоставлен невалидный URL для скрапинга: {}", urlString, e);
                return new AgentResult(
                        getName(),
                        AgentResult.Status.FAILURE,
                        "Невалидный формат URL: " + urlString,
                        Map.of()
                );
            } catch (IOException e) {
                log.error("Ошибка при скрапинге URL: {}", urlString, e);
                return new AgentResult(
                        getName(),
                        AgentResult.Status.FAILURE,
                        "Не удалось извлечь контент: " + e.getMessage(),
                        Map.of()
                );
            }
        });
    }
}
