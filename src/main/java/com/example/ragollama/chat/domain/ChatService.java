package com.example.ragollama.chat.domain;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * "Чистый" доменный сервис для обработки бизнес-логики чата.
 * <p> Этот сервис является ядром функциональности чата и строго следует
 * Принципу Единственной Ответственности (SRP). Его единственная задача — принять
 * готовые данные (полную историю диалога) и делегировать вызов LLM
 * специализированному клиенту {@link LlmClient}.
 * <p> Он не знает о сессиях, базе данных или HTTP, что делает его максимально
 * переиспользуемым, легко тестируемым в изоляции и соответствующим
 * принципам Clean Architecture.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final LlmClient llmClient;

    /**
     * Обрабатывает чат-запрос асинхронно, возвращая полный ответ.
     *
     * @param history Предоставленная история чата для поддержания контекста.
     *                Последнее сообщение в списке - это текущий запрос пользователя.
     * @return {@link CompletableFuture} с финальным ответом от LLM.
     */
    public CompletableFuture<String> processChatRequestAsync(List<Message> history) {
        log.info("Обработка 'чистого' асинхронного запроса в чат.");
        Prompt prompt = new Prompt(history);
        return llmClient.callChat(prompt, ModelCapability.BALANCED);
    }

    /**
     * Обрабатывает чат-запрос в потоковом режиме (Server-Sent Events).
     *
     * @param history Предоставленная история чата. Последнее сообщение в списке -
     *                это текущий запрос пользователя.
     * @return Реактивный поток {@link Flux}, передающий части ответа по мере их генерации.
     */
    public Flux<String> processChatRequestStream(List<Message> history) {
        log.info("Обработка 'чистого' потокового запроса в чат.");
        Prompt prompt = new Prompt(history);
        return llmClient.streamChat(prompt, ModelCapability.BALANCED);
    }
}
