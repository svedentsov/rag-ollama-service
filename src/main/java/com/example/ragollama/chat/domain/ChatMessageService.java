package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.shared.exception.AccessDeniedException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Доменный сервис для управления жизненным циклом отдельных сообщений чата.
 * <p>
 * Этот сервис инкапсулирует бизнес-логику, связанную с операциями над
 * сущностью {@link ChatMessage}, такими как обновление и удаление.
 * Он также отвечает за проверку прав доступа, гарантируя, что пользователи
 * могут изменять только сообщения в своих сессиях.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionService chatSessionService;

    /**
     * Обновляет текст сообщения, принадлежащего текущему пользователю.
     *
     * @param messageId  ID сообщения для обновления.
     * @param newContent Новый текст сообщения.
     * @throws EntityNotFoundException если сообщение не найдено.
     * @throws AccessDeniedException   если пользователь не является владельцем сессии сообщения.
     */
    public void updateMessage(UUID messageId, String newContent) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Сообщение с ID " + messageId + " не найдено."));
        // Проверяем, что пользователь является владельцем сессии, к которой относится сообщение.
        // Это единственная необходимая проверка безопасности.
        chatSessionService.findAndVerifyOwnership(message.getSessionId());
        message.setContent(newContent);
        chatMessageRepository.save(message);
    }

    /**
     * Удаляет сообщение, принадлежащее сессии текущего пользователя.
     *
     * @param messageId ID сообщения для удаления.
     * @throws EntityNotFoundException если сообщение не найдено.
     * @throws AccessDeniedException   если пользователь не является владельцем сессии сообщения.
     */
    public void deleteMessage(UUID messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Сообщение с ID " + messageId + " не найдено."));
        // Проверяем, что пользователь является владельцем сессии.
        chatSessionService.findAndVerifyOwnership(message.getSessionId());
        chatMessageRepository.delete(message);
    }
}
