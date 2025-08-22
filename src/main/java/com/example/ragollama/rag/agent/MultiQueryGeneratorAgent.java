package com.example.ragollama.rag.agent.MultiQueryGeneratorAgent;

import com.example.ragollama.rag.agent.QueryEnhancementAgent;
import com.example.ragollama.shared.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI-агент, реализующий стратегию Multi-Query.
 * <p>
 * Эта версия использует значительно улучшенный промпт с техникой "few-shot prompting"
 * (предоставление примеров), чтобы заставить LLM генерировать чистый,
 * структурированный вывод без лишних фраз и маркеров. Это повышает
 * надежность всего RAG-конвейера.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class MultiQueryGeneratorAgent implements QueryEnhancementAgent {

    private final LlmClient llmClient;

    private static final PromptTemplate MULTI_QUERY_PROMPT_TEMPLATE = new PromptTemplate("""
            Твоя задача — сгенерировать 3 альтернативных поисковых запроса на основе ИСХОДНОГО ЗАПРОСА.
            Запросы должны быть на том же языке, что и оригинал, и рассматривать вопрос с разных сторон.
            
            ПРАВИЛА ВЫВОДА:
            1. Твой ответ должен содержать ТОЛЬКО сгенерированные запросы.
            2. Каждый запрос должен быть на новой строке.
            3. НЕ добавляй нумерацию, маркеры (например, `*` или `•`), заголовки, кавычки или любые вводные/заключительные фразы.
            
            ПРИМЕР:
            ИСХОДНЫЙ ЗАПРОС: "Расскажи, как Spring Boot упрощает разработку микросервисов."
            
            ПРИМЕР ВЫВОДА:
            Преимущества Spring Boot для микросервисов
            Автоконфигурация в Spring Boot для backend-разработки
            Сравнение Spring Boot и Micronaut
            
            ИСХОДНЫЙ ЗАПРОС:
            {query}
            """);

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<List<String>> enhance(String originalQuery) {
        String promptString = MULTI_QUERY_PROMPT_TEMPLATE.render(Map.of("query", originalQuery));
        Prompt prompt = new Prompt(promptString);

        return Mono.fromFuture(llmClient.callChat(prompt))
                .map(this::parseToList)
                .map(generatedQueries -> {
                    List<String> allQueries = new ArrayList<>();
                    allQueries.add(originalQuery);
                    allQueries.addAll(generatedQueries);
                    return allQueries;
                })
                .doOnSuccess(queries ->
                        log.info("MultiQueryGeneratorAgent: сгенерировано {} запросов для '{}'",
                                queries.size(), originalQuery)
                );
    }

    /**
     * Разбирает многострочный ответ от LLM в список строк.
     *
     * @param llmResponse Сырой ответ от LLM.
     * @return Список очищенных строк.
     */
    private List<String> parseToList(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return List.of();
        }
        return Arrays.stream(llmResponse.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }
}
