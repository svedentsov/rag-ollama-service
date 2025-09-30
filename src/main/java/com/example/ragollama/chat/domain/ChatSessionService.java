package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatSession;
import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.exception.AccessDeniedException;
import com.example.ragollama.shared.task.CancellableTaskService;
import com.example.ragollama.shared.task.TaskStateService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Доменный сервис для управления жизненным циклом сессий чата.
 * <p>
 * Логика удаления (`deleteChat`) была значительно упрощена. Теперь она
 * просто вызывает `chatSessionRepository.deleteById`, а JPA и база данных
 * сами заботятся о каскадном удалении всех связанных сообщений.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AppProperties appProperties;
    private final CancellableTaskService cancellableTaskService;
    private final TaskStateService taskStateService;

    @Transactional(readOnly = true)
    public List<ChatSession> getChatsForCurrentUser() {
        List<ChatSession> sessions = chatSessionRepository.findByUserNameOrderByUpdatedAtDesc(getCurrentUsername());
        sessions.forEach(session ->
                chatMessageRepository.findBySessionId(session.getSessionId(), PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .stream()
                        .findFirst()
                        .ifPresent(session::setLastMessage)
        );
        return sessions;
    }

    public ChatSession createNewChat() {
        String username = getCurrentUsername();
        String defaultName = "Новый чат от " + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        ChatSession newSession = ChatSession.builder()
                .sessionId(UUID.randomUUID())
                .userName(username)
                .chatName(defaultName)
                .build();
        return chatSessionRepository.save(newSession);
    }

    public ChatSession findOrCreateSession(UUID sessionId) {
        if (sessionId == null) {
            return createNewChat();
        }
        return findAndVerifyOwnership(sessionId);
    }

    public void updateChatName(UUID sessionId, String newName) {
        ChatSession session = findAndVerifyOwnership(sessionId);
        session.setChatName(newName);
        chatSessionRepository.save(session);
    }

    /**
     * Удаляет сессию чата.
     * Благодаря `CascadeType.ALL` и `orphanRemoval=true`, JPA автоматически удалит
     * все связанные сообщения в рамках одной транзакции. Ручной вызов
     * `chatMessageRepository.deleteBySessionId` больше не нужен.
     *
     * @param sessionId ID сессии для удаления.
     */
    public void deleteChat(UUID sessionId) {
        // Проверка прав доступа остается
        findAndVerifyOwnership(sessionId);
        // Отменяем активную задачу, если она есть
        taskStateService.getActiveTaskIdForSession(sessionId).ifPresent(taskId -> {
            log.warn("Чат {} удаляется, отменяем связанную с ним активную задачу {}.", sessionId, taskId);
            cancellableTaskService.cancel(taskId);
        });
        taskStateService.clearSessionTask(sessionId);
        // Просто удаляем сессию. JPA позаботится об остальном.
        chatSessionRepository.deleteById(sessionId);
        log.info("Чат {} и все его сообщения были успешно удалены каскадно.", sessionId);
    }

    @Transactional(readOnly = true)
    public List<com.example.ragollama.chat.domain.model.ChatMessage> getMessagesForSession(UUID sessionId) {
        findAndVerifyOwnership(sessionId);
        int historySize = appProperties.chat().history().maxMessages();
        PageRequest pageRequest = PageRequest.of(0, historySize, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<com.example.ragollama.chat.domain.model.ChatMessage> recentMessages = chatMessageRepository.findBySessionId(sessionId, pageRequest);
        Collections.reverse(recentMessages);
        return recentMessages;
    }

    public ChatSession findAndVerifyOwnership(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Чат с ID " + sessionId + " не найден."));
        if (!session.getUserName().equals(getCurrentUsername())) {
            throw new AccessDeniedException("У вас нет доступа к этому чату.");
        }
        return session;
    }

    private String getCurrentUsername() {
        return "default-user";
    }
}
