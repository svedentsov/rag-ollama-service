package com.example.ragollama.shared.llm;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Определяет низкоуровневый контракт для прямого взаимодействия с LLM.
 * <p>Этот интерфейс является абстракцией, скрывающей конкретную реализацию
 * (например, Spring AI ChatClient), и возвращает полный объект {@link ChatResponse}
 * для доступа к метаданным (например, usage).
 */
public interface LlmGateway {

    /**
     * Выполняет не-потоковый вызов к LLM.
     *
     * @param prompt  Промпт для отправки.
     * @param options Опции, специфичные для модели (имя модели, температура и т.д.).
     * @return {@link Mono}, который по завершении будет содержать полный ответ от Spring AI.
     */
    Mono<ChatResponse> call(Prompt prompt, OllamaOptions options);

    /**
     * Выполняет потоковый вызов к LLM.
     *
     * @param prompt  Промпт для отправки.
     * @param options Опции, специфичные для модели.
     * @return {@link Flux}, который будет эмитить части ответа по мере их генерации.
     */
    Flux<ChatResponse> stream(Prompt prompt, OllamaOptions options);
}
