package com.example.ragollama.chat.domain;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;

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
     * Обрабатывает чат-запрос асинхронно, возвращая полный ответ и использованный промпт.
     *
     * @param history Предоставленная история чата для поддержания контекста.
     * @return {@link Mono} с кортежем (Tuple2), содержащим финальный ответ от LLM и сам объект промпта.
     */
    public Mono<Tuple2<String, Prompt>> processChatRequestAsync(List<Message> history) {
        log.info("Обработка 'чистого' асинхронного запроса в чат.");
        Prompt prompt = new Prompt(history);
        return llmClient.callChat(prompt, ModelCapability.BALANCED);
    }


    /**
     * Обрабатывает чат-запрос в потоковом режиме (Server-Sent Events).
     *
     * @param prompt Промпт для LLM.
     * @return Реактивный поток {@link Flux}, передающий части ответа по мере их генерации.
     */
    public Flux<String> processChatRequestStream(Prompt prompt) {
        log.info("Обработка 'чистого' потокового запроса в чат.");
        return llmClient.streamChat(prompt, ModelCapability.BALANCED);
    }
}
