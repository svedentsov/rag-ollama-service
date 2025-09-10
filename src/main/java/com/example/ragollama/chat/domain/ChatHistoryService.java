package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.chat.mappers.ChatHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для управления историей сообщений чата в асинхронном режиме.
 * <p> Этот сервис инкапсулирует всю логику взаимодействия с базой данных для
 * сохранения и извлечения истории диалогов. Все операции выполняются
 * асинхронно в выделенном пуле потоков и являются транзакционными,
 * что обеспечивает высокую производительность и консистентность данных.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatHistoryMapper chatHistoryMapper;

    /**
     * Асинхронно и транзакционно сохраняет одно сообщение в базу данных.
     * <p>Аннотация {@code @Async} указывает Spring выполнить этот метод в отдельном
     * потоке из указанного пула. Аннотация {@code @Transactional} гарантирует,
     * что выполнение будет обернуто в транзакцию.
     *
     * @param sessionId ID сессии чата.
     * @param role      Роль отправителя сообщения.
     * @param content   Текст сообщения.
     * @return {@link CompletableFuture}, который завершается после успешного сохранения сообщения.
     */
    @Async("applicationTaskExecutor")
    @Transactional
    public CompletableFuture<Void> saveMessageAsync(UUID sessionId, MessageRole role, String content) {
        ChatMessage message = chatHistoryMapper.toChatMessageEntity(sessionId, role, content);
        chatMessageRepository.save(message);
        log.debug("Сохранено сообщение для сессии {}: Role={}", sessionId, role);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Асинхронно и транзакционно загружает N последних сообщений для указанной сессии.
     * <p>  возвращается в хронологическом порядке (от старых к новым),
     * что является корректным форматом для передачи в Spring AI Prompt.
     * Операция выполняется в отдельном потоке с активной транзакцией.
     *
     * @param sessionId ID сессии чата.
     * @param lastN     Количество последних сообщений для загрузки.
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * список сообщений {@link Message}, готовый к использованию в промпте.
     */
    @Async("applicationTaskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<List<Message>> getLastNMessagesAsync(UUID sessionId, int lastN) {
        if (lastN <= 0) {
            return CompletableFuture.completedFuture(List.of());
        }
        PageRequest pageRequest = PageRequest.of(0, lastN, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ChatMessage> recentMessages = chatMessageRepository.findBySessionId(sessionId, pageRequest);
        log.debug("Загружено {} сообщений для сессии {}", recentMessages.size(), sessionId);

        List<Message> springAiMessages = chatHistoryMapper.toSpringAiMessages(recentMessages);
        return CompletableFuture.completedFuture(springAiMessages);
    }
}
