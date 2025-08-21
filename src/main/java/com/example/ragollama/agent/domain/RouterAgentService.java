package com.example.ragollama.agent.domain;

import com.example.ragollama.shared.llm.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Сервис, реализующий логику "Router Agent".
 * Классифицирует запрос пользователя для направления в соответствующий конвейер.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouterAgentService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    // Промпт заставляет LLM классифицировать запрос и вернуть ТОЛЬКО JSON
    private static final PromptTemplate ROUTER_PROMPT_TEMPLATE = new PromptTemplate("""
            Твоя задача - проанализировать ЗАПРОС ПОЛЬЗОВАТЕЛЯ и определить его намерение.
            Ответь ТОЛЬКО одним словом, соответствующим одному из следующих типов:
            - CHITCHAT: если это приветствие, прощание, благодарность или вопрос не по теме.
            - RAG_QUERY: если пользователь задает вопрос, для ответа на который, скорее всего, нужна информация из базы знаний.
            - CODE_GENERATION: если пользователь явно просит написать код, скрипт или тест.
            
            Примеры:
            - Запрос: "Привет, как дела?" -> Ответ: CHITCHAT
            - Запрос: "Что такое Spring AI?" -> Ответ: RAG_QUERY
            - Запрос: "Напиши тест для /api/users" -> Ответ: CODE_GENERATION
            
            ЗАПРОС ПОЛЬЗОВАТЕЛЯ:
            {query}
            """);

    /**
     * Определяет намерение пользователя по тексту его запроса.
     *
     * @param query Текст запроса.
     * @return {@link Mono} с определенным {@link QueryIntent}.
     */
    public Mono<QueryIntent> route(String query) {
        String promptString = ROUTER_PROMPT_TEMPLATE.render(Map.of("query", query));
        Prompt prompt = new Prompt(promptString);

        return Mono.fromFuture(llmClient.callChat(prompt))
                .map(response -> {
                    try {
                        // Просто парсим одно слово как enum
                        return QueryIntent.valueOf(response.trim().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("RouterAgent не смог распознать намерение из ответа LLM: '{}'. Используется fallback.", response);
                        return QueryIntent.RAG_QUERY; // Безопасный fallback
                    }
                })
                .doOnSuccess(intent -> log.info("Запрос классифицирован с намерением: {}", intent));
    }
}
