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

import java.util.StringJoiner;

/**
 * Стандартная реализация {@link LlmGateway}, использующая Spring AI {@link ChatClient}.
 * <p> Этот компонент отвечает исключительно за прямое, "сырое" взаимодействие с LLM.
 * Он выполняется в выделенном пуле потоков для I/O-операций и включает
 * детализированное логирование параметров вызова для улучшения наблюдаемости.
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
        log.debug("Выполнение не-потокового вызова к LLM. {}", formatOptionsForLogging(options));
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
        log.debug("Выполнение потокового вызова к LLM. {}", formatOptionsForLogging(options));
        return Flux.defer(() -> chatClient.prompt(prompt)
                        .options(options)
                        .stream()
                        .chatResponse())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Форматирует объект {@link OllamaOptions} в читаемую строку для логирования.
     * <p> Этот метод решает проблему неинформативного стандартного `toString()`
     * и позволяет видеть в логах все ключевые параметры, с которыми
     * был выполнен вызов к LLM.
     *
     * @param options Объект с опциями для модели.
     * @return Строка вида "Параметры: [model=llama3, temperature=0.7, format=json]".
     */
    private String formatOptionsForLogging(OllamaOptions options) {
        if (options == null) {
            return "Параметры: []";
        }

        StringJoiner joiner = new StringJoiner(", ", "Параметры: [", "]");
        if (options.getModel() != null) {
            joiner.add("model=" + options.getModel());
        }
        if (options.getTemperature() != null) {
            joiner.add("temperature=" + options.getTemperature());
        }
        if (options.getTopK() != null) {
            joiner.add("topK=" + options.getTopK());
        }
        if (options.getTopP() != null) {
            joiner.add("topP=" + options.getTopP());
        }
        if (options.getFormat() != null) {
            joiner.add("format=" + options.getFormat());
        }
        return joiner.toString();
    }
}
