package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.ChatSession;
import com.example.ragollama.shared.config.properties.AppProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AppProperties appProperties;

    /**
     * Возвращает список всех чатов для текущего аутентифицированного пользователя.
     *
     * @return Список сущностей ChatSession.
     */
    @Transactional(readOnly = true)
    public List<ChatSession> getChatsForCurrentUser() {
        return chatSessionRepository.findByUserNameOrderByUpdatedAtDesc(getCurrentUsername());
    }

    /**
     * Создает новый чат для текущего пользователя с именем по умолчанию.
     *
     * @return Сохраненная сущность нового чата.
     */
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

    /**
     * Находит существующую сессию по ID или создает новую, если ID не предоставлен.
     * Также проверяет, что существующая сессия принадлежит текущему пользователю.
     *
     * @param sessionId ID сессии для поиска.
     * @return Существующая или новая сущность ChatSession.
     */
    public ChatSession findOrCreateSession(UUID sessionId) {
        if (sessionId == null) {
            return createNewChat();
        }
        return findAndVerifyOwnership(sessionId);
    }

    /**
     * Обновляет имя указанного чата, предварительно проверив,
     * что он принадлежит текущему пользователю.
     *
     * @param sessionId ID чата для обновления.
     * @param newName   Новое имя.
     */
    public void updateChatName(UUID sessionId, String newName) {
        ChatSession session = findAndVerifyOwnership(sessionId);
        session.setChatName(newName);
        chatSessionRepository.save(session);
    }

    /**
     * Удаляет чат и все связанные с ним сообщения, предварительно
     * проверив, что он принадлежит текущему пользователю.
     *
     * @param sessionId ID чата для удаления.
     */
    public void deleteChat(UUID sessionId) {
        findAndVerifyOwnership(sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
    }

    /**
     * Возвращает историю сообщений для указанного чата, проверив права доступа.
     * Этот метод напрямую запрашивает данные из репозитория и возвращает их
     * в правильном для UI порядке.
     *
     * @param sessionId ID чата.
     * @return Список сообщений в хронологическом порядке.
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesForSession(UUID sessionId) {
        findAndVerifyOwnership(sessionId); // Проверка, что пользователь владеет этим чатом
        int historySize = appProperties.chat().history().maxMessages();

        // Запрашиваем N последних сообщений (они будут отсортированы от новых к старым)
        PageRequest pageRequest = PageRequest.of(0, historySize, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ChatMessage> recentMessages = chatMessageRepository.findBySessionId(sessionId, pageRequest);

        // Переворачиваем список, чтобы он был в хронологическом порядке (старые -> новые) для отображения в UI
        Collections.reverse(recentMessages);

        return recentMessages;
    }

    /**
     * Вспомогательный метод для поиска сессии и проверки, что она
     * принадлежит текущему пользователю.
     *
     * @param sessionId ID сессии.
     * @return Найденная сущность ChatSession.
     * @throws EntityNotFoundException если сессия не найдена.
     * @throws AccessDeniedException   если сессия принадлежит другому пользователю.
     */
    private ChatSession findAndVerifyOwnership(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Чат с ID " + sessionId + " не найден."));

        if (!session.getUserName().equals(getCurrentUsername())) {
            throw new AccessDeniedException("Доступ к чату запрещен.");
        }
        return session;
    }

    /**
     * Получает имя текущего аутентифицированного пользователя из SecurityContext.
     *
     * @return Имя пользователя (username).
     */
    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
