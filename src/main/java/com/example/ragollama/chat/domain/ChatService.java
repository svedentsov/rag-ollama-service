package com.example.ragollama.chat.domain;

import com.example.ragollama.shared.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * "Чистый" сервис для обработки бизнес-логики чата.
 * <p>
 * В этой версии сервис полностью освобожден от ответственности за управление
 * сессиями и персистентностью. Его задача — принять сообщение и историю,
 * вызвать LLM и вернуть сгенерированный ответ. Он является stateless-компонентом,
 * что значительно упрощает его тестирование и переиспользование.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final LlmClient llmClient;

    /**
     * Обрабатывает чат-запрос асинхронно, возвращая полный ответ.
     *
     * @param userMessage Сообщение от пользователя.
     * @param history     Предоставленная история чата для поддержания контекста.
     * @return {@link CompletableFuture} с финальным ответом от LLM.
     */
    public CompletableFuture<String> processChatRequestAsync(String userMessage, List<Message> history) {
        log.info("Обработка 'чистого' асинхронного запроса в чат.");
        Prompt prompt = new Prompt(history);
        return llmClient.callChat(prompt);
    }

    /**
     * Обрабатывает чат-запрос в потоковом режиме (Server-Sent Events).
     *
     * @param userMessage Сообщение от пользователя.
     * @param history     Предоставленная история чата.
     * @return Реактивный поток {@link Flux}, передающий части ответа по мере их генерации.
     */
    public Flux<String> processChatRequestStream(String userMessage, List<Message> history) {
        log.info("Обработка 'чистого' потокового запроса в чат.");
        Prompt prompt = new Prompt(history);
        return llmClient.streamChat(prompt);
    }
}
