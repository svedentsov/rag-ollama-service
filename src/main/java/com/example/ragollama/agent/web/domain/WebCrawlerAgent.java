package com.example.ragollama.agent.web.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент-инструмент для скрапинга (веб-кроулинга) веб-страниц.
 * <p>
 * Использует библиотеку Jsoup для извлечения чистого текста со страницы по URL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebCrawlerAgent implements ToolAgent {

    @Override
    public String getName() {
        return "web-crawler";
    }

    @Override
    public String getDescription() {
        return "Извлекает текстовое содержимое веб-страницы по ее URL.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("url");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            String url = (String) context.payload().get("url");
            log.info("WebCrawlerAgent: запуск скрапинга для URL: {}", url);
            try {
                String textContent = Jsoup.connect(url).get().text();
                String summary = "Скрапинг успешно завершен. Извлечено символов: " + textContent.length();
                return new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        summary,
                        Map.of("scrapedText", textContent)
                );
            } catch (IOException e) {
                log.error("Ошибка при скрапинге URL: {}", url, e);
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
