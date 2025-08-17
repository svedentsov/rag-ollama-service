package com.example.ragollama.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Multi-Query Generator: генерирует несколько вариантов исходного запроса
 * для повышения полноты поиска (recall).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MultiQueryGeneratorService {

    private final LlmClient llmClient;

    private static final PromptTemplate MULTI_QUERY_PROMPT_TEMPLATE = new PromptTemplate("""
            Вы — полезный ИИ-ассистент. Сгенерируйте 3 альтернативных запроса
            на основе исходного. Запросы должны быть на том же языке.
            Только список строк без нумерации.
            
            Оригинальный запрос: {query}
            """);

    /**
     * Асинхронно генерирует список запросов (оригинал + 3 варианта).
     */
    public Mono<List<String>> generate(String originalQuery) {
        String promptString = MULTI_QUERY_PROMPT_TEMPLATE.render(Map.of("query", originalQuery));
        Prompt prompt = new Prompt(promptString);

        return Mono.fromFuture(llmClient.callChat(prompt)) // callChat возвращает CompletableFuture<String>
                .map(this::parseToList)
                .map(generatedQueries -> {
                    List<String> allQueries = new java.util.ArrayList<>();
                    allQueries.add(originalQuery);        // добавляем оригинальный
                    allQueries.addAll(generatedQueries);  // + сгенерированные
                    return allQueries;
                })
                .doOnSuccess(queries ->
                        log.info("Сгенерировано {} альтернативных запросов для '{}'",
                                queries.size(), originalQuery)
                );
    }

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
