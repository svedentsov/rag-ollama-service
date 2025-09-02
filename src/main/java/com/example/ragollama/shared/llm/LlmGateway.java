package com.example.ragollama.shared.llm;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Определяет контракт для низкоуровневого шлюза к языковой модели.
 * <p>
 * Этот интерфейс является абстракцией над конкретной реализацией AI-клиента
 * (в данном случае, Spring AI ChatClient). Он отвечает исключительно за
 * отправку запроса и получение "сырого" ответа, не содержа логики
 * отказоустойчивости или оркестрации.
 */
public interface LlmGateway {

    /**
     * Выполняет не-потоковый вызов к LLM.
     *
     * @param prompt  Промпт для отправки.
     * @param options Опции, специфичные для модели (например, имя модели, температура).
     * @return {@link Mono}, который по завершении будет содержать полный ответ.
     */
    Mono<String> call(Prompt prompt, OllamaOptions options);

    /**
     * Выполняет потоковый вызов к LLM.
     *
     * @param prompt  Промпт для отправки.
     * @param options Опции, специфичные для модели.
     * @return {@link Flux}, который будет эмитить части ответа по мере их генерации.
     */
    Flux<String> stream(Prompt prompt, OllamaOptions options);
}
