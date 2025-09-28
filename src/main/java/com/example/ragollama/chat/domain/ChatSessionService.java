package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.ChatSession;
import com.example.ragollama.shared.config.properties.AppProperties;
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
import java.util.stream.Collectors;

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

        // ПРИМЕЧАНИЕ: Это вызывает проблему N+1 запросов. В production-системе
        // это следует оптимизировать одним сложным SQL-запросом.
        // Для демонстрации оставляем так для простоты.
        sessions.forEach(session -> {
            chatMessageRepository.findBySessionId(session.getSessionId(), PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                    .stream()
                    .findFirst()
                    .ifPresent(session::setLastMessage);
        });

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

    // ... остальные методы без изменений ...

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

    public void deleteChat(UUID sessionId) {
        findAndVerifyOwnership(sessionId);
        taskStateService.getActiveTaskIdForSession(sessionId).ifPresent(taskId -> {
            log.warn("Чат {} удаляется, отменяем связанную с ним активную задачу {}.", sessionId, taskId);
            boolean wasCancelled = cancellableTaskService.cancel(taskId);
            if (wasCancelled) {
                log.info("Активная задача {} для удаляемого чата {} успешно отменена.", taskId, sessionId);
            } else {
                log.warn("Не удалось отменить задачу {} для чата {}, возможно, она уже завершена.", taskId, sessionId);
            }
        });
        taskStateService.clearSessionTask(sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
        log.info("Чат {} и все его сообщения были успешно удалены.", sessionId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesForSession(UUID sessionId) {
        findAndVerifyOwnership(sessionId);
        int historySize = appProperties.chat().history().maxMessages();
        PageRequest pageRequest = PageRequest.of(0, historySize, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ChatMessage> recentMessages = chatMessageRepository.findBySessionId(sessionId, pageRequest);
        Collections.reverse(recentMessages);
        return recentMessages;
    }

    private ChatSession findAndVerifyOwnership(UUID sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Чат с ID " + sessionId + " не найден."));
    }

    private String getCurrentUsername() {
        return "default-user";
    }
}
