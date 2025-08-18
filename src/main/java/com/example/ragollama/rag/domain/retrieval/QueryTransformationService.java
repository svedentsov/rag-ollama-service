package com.example.ragollama.rag.domain.retrieval;

import com.example.ragollama.shared.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Сервис, отвечающий за трансформацию исходного запроса пользователя
 * в оптимизированную версию для векторного поиска (Query Extension).
 * <p>
 * Эта версия содержит улучшенный промпт, который явно указывает LLM
 * отвечать на том же языке, что и исходный запрос, и фокусируется
 * на извлечении ключевой идеи, а не на полном переформулировании.
 * Это предотвращает "языковой барьер" при поиске в одноязычной векторной базе.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QueryTransformationService {

    private final LlmClient llmClient;

    private final PromptTemplate queryRewriteTemplate = new PromptTemplate("""
            Твоя задача — проанализировать вопрос пользователя и извлечь из него основную, самую важную идею для поиска.
            Удали все лишнее: приветствия, вводные слова, мета-вопросы. Оставь только суть.
            ВАЖНО: Ответь НА РУССКОМ ЯЗЫКЕ.
            Твой ответ должен быть коротким и содержать только переформулированную идею запроса.
            
            Пример:
            Вопрос: "Привет, не мог бы ты мне рассказать, что такое Spring Boot и для чего он нужен?"
            Ответ: "определение и назначение Spring Boot"
            
            Вопрос пользователя: "{query}"
            """);

    /**
     * Асинхронно трансформирует пользовательский запрос с помощью LLM.
     *
     * @param originalQuery Оригинальный, "сырой" запрос от пользователя.
     * @return {@link Mono}, который по завершении асинхронной операции эммитит
     *         трансформированный запрос в виде строки.
     */
    public Mono<String> transform(String originalQuery) {
        log.debug("Начало трансформации запроса: '{}'", originalQuery);
        String promptString = queryRewriteTemplate.render(Map.of("query", originalQuery));
        Prompt prompt = new Prompt(promptString);

        return Mono.fromFuture(llmClient.callChat(prompt))
                .map(response -> response.replaceAll("\"", "").trim()) // Очищаем от кавычек и пробелов
                .doOnSuccess(transformedQuery -> log.info("Запрос '{}' трансформирован в '{}'", originalQuery, transformedQuery))
                .doOnError(error -> log.error("Ошибка при трансформации запроса: '{}'", originalQuery, error));
    }
}
