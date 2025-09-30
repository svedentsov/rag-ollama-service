package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.ChatSession;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.chat.mappers.ChatHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-адаптер для персистентности истории чата.
 * <p>
 * Отвечает за асинхронное сохранение и извлечение сообщений.
 * Метод `saveMessageAsync` теперь принимает сущность `ChatSession` для
 * корректной синхронизации двунаправленной связи.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ChatSessionRepository chatSessionRepository; // Добавлена зависимость

    /**
     * Асинхронно сохраняет одно сообщение в базу данных.
     *
     * @param session  Сущность сессии, к которой относится сообщение.
     * @param role     Роль отправителя (USER или ASSISTANT).
     * @param content  Текст сообщения.
     * @param parentId Опциональный ID родительского сообщения.
     * @return {@link CompletableFuture} с сохраненной сущностью {@link ChatMessage}.
     */
    @Async("databaseTaskExecutor")
    @Transactional
    public CompletableFuture<ChatMessage> saveMessageAsync(ChatSession session, MessageRole role, String content, UUID parentId) {
        // Важно: сначала получаем управляемую (managed) сущность сессии
        ChatSession managedSession = chatSessionRepository.findById(session.getSessionId())
                .orElseThrow(() -> new IllegalStateException("Session not found for saving message"));
        ChatMessage message = chatHistoryMapper.toChatMessageEntity(managedSession, role, content, parentId);
        // Используем helper-метод для синхронизации связи
        managedSession.addMessage(message);
        // Сохраняем сессию, `cascade` позаботится о сообщении
        chatSessionRepository.save(managedSession);
        log.debug("Сохранено сообщение для сессии {}: Role={}, ParentId={}", session.getSessionId(), role, parentId);
        // Ищем сохраненное сообщение, чтобы вернуть его с ID
        ChatMessage savedMessage = managedSession.getMessages().get(managedSession.getMessages().size() - 1);
        return CompletableFuture.completedFuture(savedMessage);
    }

    @Async("databaseTaskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<List<Message>> getLastNMessagesAsync(UUID sessionId, int lastN) {
        if (lastN <= 0) {
            return CompletableFuture.completedFuture(List.of());
        }
        Pageable pageable = PageRequest.of(0, lastN, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ChatMessage> recentMessages = chatMessageRepository.findBySessionId(sessionId, pageable);
        log.debug("Загружено {} сообщений для сессии {}", recentMessages.size(), sessionId);

        List<Message> springAiMessages = chatHistoryMapper.toSpringAiMessages(recentMessages);
        return CompletableFuture.completedFuture(springAiMessages);
    }
}
