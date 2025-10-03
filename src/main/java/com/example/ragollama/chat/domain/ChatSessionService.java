package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatSession;
import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.exception.AccessDeniedException;
import com.example.ragollama.shared.task.TaskLifecycleService;
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
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AppProperties appProperties;
    private final TaskLifecycleService taskLifecycleService;

    /**
     * Возвращает список всех сессий для текущего пользователя.
     * @return Список сущностей {@link ChatSession}.
     */
    @Transactional(readOnly = true)
    public List<ChatSession> getChatsForCurrentUser() {
        List<ChatSession> sessions = chatSessionRepository.findByUserNameOrderByUpdatedAtDesc(getCurrentUsername());
        // Обогащаем сессии последним сообщением для отображения в UI
        sessions.forEach(session ->
                chatMessageRepository.findTopBySessionSessionIdOrderByCreatedAtDesc(session.getSessionId())
                        .ifPresent(session::setLastMessage)
        );
        return sessions;
    }

    /**
     * Создает новую сессию чата для текущего пользователя.
     * Генерация ID теперь делегирована JPA.
     * @return Сохраненная сущность {@link ChatSession}.
     */
    public ChatSession createNewChat() {
        String username = getCurrentUsername();
        String defaultName = "Новый чат от " + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        ChatSession newSession = ChatSession.builder()
                .userName(username)
                .chatName(defaultName)
                .build(); // ID больше не устанавливается вручную

        return chatSessionRepository.save(newSession);
    }

    /**
     * Находит существующую сессию по ID или создает новую, если ID null.
     * @param sessionId ID сессии или null.
     * @return Существующая или новая {@link ChatSession}.
     */
    public ChatSession findOrCreateSession(UUID sessionId) {
        if (sessionId == null) {
            return createNewChat();
        }
        return findAndVerifyOwnership(sessionId);
    }

    /**
     * Обновляет имя сессии чата.
     * @param sessionId ID сессии.
     * @param newName Новое имя.
     */
    public void updateChatName(UUID sessionId, String newName) {
        ChatSession session = findAndVerifyOwnership(sessionId);
        session.setChatName(newName);
        chatSessionRepository.save(session);
    }

    /**
     * Удаляет сессию чата, атомарно блокируя запись и отменяя связанную задачу.
     * @param sessionId ID сессии для удаления.
     */
    public void deleteChat(UUID sessionId) {
        ChatSession session = chatSessionRepository.findByIdWithLock(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Чат с ID " + sessionId + " не найден."));

        if (!session.getUserName().equals(getCurrentUsername())) {
            throw new AccessDeniedException("У вас нет доступа к этому чату.");
        }

        taskLifecycleService.getActiveTaskForSession(sessionId).ifPresent(task -> {
            log.warn("Чат {} удаляется, отменяем связанную с ним активную задачу {}.", sessionId, task.getId());
            taskLifecycleService.cancel(task.getId());
        });

        // Удаление сущности ChatSession автоматически удалит все связанные ChatMessage
        // благодаря `cascade = CascadeType.ALL` и `orphanRemoval = true`
        chatSessionRepository.delete(session);
        log.info("Чат {} и все его сообщения были успешно удалены.", sessionId);
    }

    /**
     * Возвращает историю сообщений для указанной сессии.
     * @param sessionId ID сессии.
     * @return Список сообщений.
     */
    @Transactional(readOnly = true)
    public List<com.example.ragollama.chat.domain.model.ChatMessage> getMessagesForSession(UUID sessionId) {
        findAndVerifyOwnership(sessionId);
        int historySize = appProperties.chat().history().maxMessages();
        PageRequest pageRequest = PageRequest.of(0, historySize, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<com.example.ragollama.chat.domain.model.ChatMessage> recentMessages = chatMessageRepository.findBySessionId(sessionId, pageRequest);
        Collections.reverse(recentMessages);
        return recentMessages;
    }

    /**
     * Находит сессию и проверяет, что она принадлежит текущему пользователю.
     * @param sessionId ID сессии.
     * @return Найденная {@link ChatSession}.
     * @throws EntityNotFoundException если сессия не найдена.
     * @throws AccessDeniedException если сессия принадлежит другому пользователю.
     */
    public ChatSession findAndVerifyOwnership(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Чат с ID " + sessionId + " не найден."));
        if (!session.getUserName().equals(getCurrentUsername())) {
            throw new AccessDeniedException("У вас нет доступа к этому чату.");
        }
        return session;
    }

    private String getCurrentUsername() {
        // Заглушка, в реальном приложении здесь будет логика получения пользователя из SecurityContext
        return "default-user";
    }
}
