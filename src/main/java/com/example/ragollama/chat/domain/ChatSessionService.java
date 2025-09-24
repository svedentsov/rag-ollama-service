package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.ChatSession;
import com.example.ragollama.shared.config.properties.AppProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
 * Сервис для управления жизненным циклом сессий чата и их сообщениями.
 * Этот сервис инкапсулирует всю бизнес-логику, связанную с чатами,
 * такую как создание, поиск, обновление и удаление сессий. Он также
 * предоставляет методы для доступа к истории сообщений, соблюдая
 * правила пагинации и владения данными.
 * В данной версии сервиса отсутствует явная аутентификация, и все
 * операции выполняются от имени пользователя по умолчанию.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AppProperties appProperties;

    /**
     * Возвращает список всех чатов для текущего пользователя.
     * Поскольку аутентификация удалена, все чаты относятся к одному
     * пользователю по умолчанию.
     *
     * @return Список сущностей ChatSession.
     */
    @Transactional(readOnly = true)
    public List<ChatSession> getChatsForCurrentUser() {
        return chatSessionRepository.findByUserNameOrderByUpdatedAtDesc(getCurrentUsername());
    }

    /**
     * Создает новый чат для пользователя по умолчанию.
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
        return findAndVerifyOwnership(sessionId);
    }

    /**
     * Обновляет имя указанного чата.
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
     * Удаляет чат и все связанные с ним сообщения.
     *
     * @param sessionId ID чата для удаления.
     */
    public void deleteChat(UUID sessionId) {
        findAndVerifyOwnership(sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
    }

    /**
     * Возвращает историю сообщений для указанного чата.
     *
     * @param sessionId ID чата.
     * @return Список сообщений в хронологическом порядке.
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesForSession(UUID sessionId) {
        findAndVerifyOwnership(sessionId); // Проверка, что сессия существует
        int historySize = appProperties.chat().history().maxMessages();
        PageRequest pageRequest = PageRequest.of(0, historySize, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ChatMessage> recentMessages = chatMessageRepository.findBySessionId(sessionId, pageRequest);
        Collections.reverse(recentMessages);
        return recentMessages;
    }

    /**
     * Вспомогательный метод для поиска сессии.
     *
     * @param sessionId ID сессии.
     * @return Найденная сущность ChatSession.
     * @throws EntityNotFoundException если сессия не найдена.
     */
    private ChatSession findAndVerifyOwnership(UUID sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Чат с ID " + sessionId + " не найден."));
    }

    /**
     * Возвращает имя пользователя по умолчанию, так как аутентификация удалена.
     *
     * @return Имя пользователя "default-user".
     */
    private String getCurrentUsername() {
        return "default-user";
    }
}
