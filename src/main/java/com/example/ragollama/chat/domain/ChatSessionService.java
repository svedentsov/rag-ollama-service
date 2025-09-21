package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.ChatSession;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.shared.config.properties.AppProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatHistoryService chatHistoryService;
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
     *
     * @param sessionId ID сессии для поиска.
     * @return Существующая или новая сущность ChatSession.
     */
    public ChatSession findOrCreateSession(UUID sessionId) {
        if (sessionId == null) {
            return createNewChat();
        }
        return chatSessionRepository.findById(sessionId).orElseGet(this::createNewChat);
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
     *
     * @param sessionId ID чата.
     * @return Список сообщений.
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesForSession(UUID sessionId) {
        findAndVerifyOwnership(sessionId); // Проверка, что пользователь владеет этим чатом
        int historySize = appProperties.chat().history().maxMessages();

        // Используем CompletableFuture.join() для синхронного получения результата
        // внутри транзакционного метода, так как нам не нужна здесь дальнейшая асинхронность.
        List<Message> springAiMessages = chatHistoryService.getLastNMessagesAsync(sessionId, historySize).join();

        // Преобразуем из Spring AI Message в нашу сущность для передачи в DTO
        return springAiMessages.stream()
                .map(msg -> ChatMessage.builder()
                        .role(MessageRole.valueOf(msg.getMessageType().name()))
                        .content(msg.getText())
                        .build())
                .toList();
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
