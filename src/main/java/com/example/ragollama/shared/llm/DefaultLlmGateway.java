package com.example.ragollama.shared.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Стандартная реализация {@link LlmGateway}, использующая Spring AI ChatClient.
 * <p>
 * Эта версия возвращает полный объект {@link ChatResponse}, содержащий как
 * контент, так и метаданные (включая Usage), что необходимо для работы
 * системы контроля квот.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultLlmGateway implements LlmGateway {

    private final ChatClient chatClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ChatResponse> call(Prompt prompt, OllamaOptions options) {
        log.debug("Выполнение не-потокового вызова к LLM модели: {}", options.getModel());
        return Mono.fromCallable(() -> chatClient.prompt(prompt)
                        .options(options)
                        .call()
                        .chatResponse())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<ChatResponse> stream(Prompt prompt, OllamaOptions options) {
        log.debug("Выполнение потокового вызова к LLM модели: {}", options.getModel());
        return Flux.defer(() -> chatClient.prompt(prompt)
                        .options(options)
                        .stream()
                        .chatResponse())
                .subscribeOn(Schedulers.boundedElastic());
    }
}
