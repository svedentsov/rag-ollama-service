package com.example.ragollama.rag.agent;

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
 * AI-агент, реализующий стратегию Multi-Query. Он генерирует несколько
 * альтернативных вариантов исходного запроса для повышения полноты поиска (recall).
 * Аннотация {@code @Order(20)} задает его приоритет в конвейере агентов,
 * он будет выполнен после агентов с меньшим порядковым номером.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class MultiQueryGeneratorAgent implements QueryEnhancementAgent {

    private final LlmClient llmClient;

    private static final PromptTemplate MULTI_QUERY_PROMPT_TEMPLATE = new PromptTemplate("""
            Вы — полезный ИИ-ассистент. Сгенерируйте 3 альтернативных запроса
            на основе исходного. Запросы должны быть на том же языке.
            Ответ должен содержать только список строк без нумерации.
            
            Оригинальный запрос: {query}
            """);

    /**
     * {@inheritDoc}
     * <p>
     * Генерирует список, включающий оригинальный запрос и 3 сгенерированных варианта.
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
