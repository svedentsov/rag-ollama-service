package com.example.ragollama.rag.agent;

import com.example.ragollama.shared.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AI-агент, отвечающий за трансформацию исходного запроса пользователя
 * в оптимизированную версию для векторного поиска (Query Transformation/Extension).
 * Его задача — удалить из запроса "шум" (приветствия, мета-вопросы) и сфокусировать его на ключевой идее.
 * Аннотация {@code @Order(10)} гарантирует, что этот агент будет выполнен одним из первых в конвейере.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class QueryTransformationAgent implements QueryEnhancementAgent {

    private final LlmClient llmClient;

    private final PromptTemplate queryRewriteTemplate = new PromptTemplate("""
            Твоя задача — проанализировать вопрос пользователя и извлечь из него основную, самую важную идею для поиска.
            Удали все лишнее: приветствия, вводные слова, мета-вопросы. Оставь только суть.
            ВАЖНО: Ответь НА ТОМ ЖЕ ЯЗЫКЕ, что и вопрос.
            Твой ответ должен быть коротким и содержать только переформулированную идею запроса.
            
            Пример:
            Вопрос: "Привет, не мог бы ты мне рассказать, что такое Spring Boot и для чего он нужен?"
            Ответ: "определение и назначение Spring Boot"
            
            Вопрос пользователя: "{query}"
            """);

    /**
     * {@inheritDoc}
     * <p>
     * Возвращает список, содержащий только один, но трансформированный запрос.
     */
    @Override
    public Mono<List<String>> enhance(String originalQuery) {
        log.debug("QueryTransformationAgent: начало трансформации запроса: '{}'", originalQuery);
        String promptString = queryRewriteTemplate.render(Map.of("query", originalQuery));
        Prompt prompt = new Prompt(promptString);

        return Mono.fromFuture(llmClient.callChat(prompt))
                .map(response -> response.replaceAll("\"", "").trim())
                .map(List::of) // Оборачиваем результат в список
                .doOnSuccess(transformedQuery -> log.info("QueryTransformationAgent: запрос '{}' трансформирован в '{}'",
                        originalQuery, transformedQuery.getFirst()));
    }
}
