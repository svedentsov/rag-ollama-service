package com.example.ragollama.shared.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Стандартная реализация {@link LlmGateway}, использующая Spring AI ChatClient.
 * <p>
 * Этот компонент является "последней милей" перед вызовом LLM. Он отвечает за
 * правильную конфигурацию вызова и его выполнение на отдельном, неблокирующем
 * пуле потоков. В этой версии он получает готовый {@link ChatClient} через
 * DI, а не строит его самостоятельно.
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
    public Mono<String> call(Prompt prompt, OllamaOptions options) {
        log.debug("Выполнение не-потокового вызова к LLM модели: {}", options.getModel());
        return Mono.fromCallable(() -> chatClient.prompt(prompt)
                        .options(options)
                        .call()
                        .content())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<String> stream(Prompt prompt, OllamaOptions options) {
        log.debug("Выполнение потокового вызова к LLM модели: {}", options.getModel());
        return Flux.defer(() -> chatClient.prompt(prompt)
                        .options(options)
                        .stream()
                        .content())
                .subscribeOn(Schedulers.boundedElastic());
    }
}
