package com.example.ragollama.agent.domain;

import com.example.ragollama.shared.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * Сервис, реализующий логику "Router Agent".
 * <p>
 * Классифицирует запрос пользователя для направления в соответствующий конвейер.
 * Эта версия использует усиленный промпт и более надежный механизм парсинга
 * ответа от LLM для повышения стабильности и предсказуемости.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouterAgentService {

    private final LlmClient llmClient;

    /**
     * Промпт, который заставляет LLM классифицировать запрос и вернуть ТОЛЬКО одно из
     * предопределенных ключевых слов, соответствующих значениям enum {@link QueryIntent}.
     * Такой подход значительно надежнее, чем просьба вернуть JSON.
     */
    private static final PromptTemplate ROUTER_PROMPT_TEMPLATE = new PromptTemplate("""
            Твоя задача - проанализировать ЗАПРОС ПОЛЬЗОВАТЕЛЯ и определить его намерение.
            Ответь ТОЛЬКО ОДНИМ СЛОВОМ из следующего списка:
            - CHITCHAT
            - RAG_QUERY
            - CODE_GENERATION
            
            Примеры:
            - Запрос: "Привет, как дела?" -> Ответ: CHITCHAT
            - Запрос: "Что такое Spring AI?" -> Ответ: RAG_QUERY
            - Запрос: "Напиши тест для /api/users" -> Ответ: CODE_GENERATION
            
            ЗАПРОС ПОЛЬЗОВАТЕЛЯ:
            {query}
            """);

    /**
     * Определяет намерение пользователя по тексту его запроса.
     * <p>
     * Метод использует усиленный промпт и безопасный парсинг ответа, чтобы избежать
     * {@link IllegalArgumentException} и сделать логику более устойчивой к
     * незначительным отклонениям в ответах LLM. В случае невозможности
     * распознать ответ, используется безопасный fallback-вариант {@link QueryIntent#UNKNOWN}.
     *
     * @param query Текст запроса от пользователя.
     * @return {@link Mono} с определенным {@link QueryIntent}.
     */
    public Mono<QueryIntent> route(String query) {
        String promptString = ROUTER_PROMPT_TEMPLATE.render(Map.of("query", query));
        Prompt prompt = new Prompt(promptString);

        return Mono.fromFuture(llmClient.callChat(prompt))
                .map(this::parseIntentFromLlmResponse)
                .map(Optional::get) // Мы доверяем, что parse... всегда вернет значение
                .doOnSuccess(intent -> log.info("Запрос классифицирован с намерением: {}", intent));
    }

    /**
     * Безопасно парсит строковый ответ от LLM в {@link QueryIntent}.
     *
     * @param response Ответ от LLM.
     * @return {@link Optional}, содержащий {@link QueryIntent}, или fallback-значение.
     */
    private Optional<QueryIntent> parseIntentFromLlmResponse(String response) {
        if (response == null || response.isBlank()) {
            log.warn("RouterAgent получил пустой ответ от LLM. Используется fallback UNKNOWN.");
            return Optional.of(QueryIntent.UNKNOWN);
        }

        String cleanedResponse = response.trim().toUpperCase();

        if (cleanedResponse.contains("RAG_QUERY")) {
            return Optional.of(QueryIntent.RAG_QUERY);
        }
        if (cleanedResponse.contains("CHITCHAT")) {
            return Optional.of(QueryIntent.CHITCHAT);
        }
        if (cleanedResponse.contains("CODE_GENERATION")) {
            return Optional.of(QueryIntent.CODE_GENERATION);
        }

        log.warn("RouterAgent не смог распознать намерение из ответа LLM: '{}'. Используется fallback UNKNOWN.", response);
        return Optional.of(QueryIntent.UNKNOWN); // Более честный fallback
    }
}
