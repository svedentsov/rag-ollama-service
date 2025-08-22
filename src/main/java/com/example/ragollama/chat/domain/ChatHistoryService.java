package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.chat.mappers.ChatHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления историей сообщений чата.
 * <p>
 * Эта версия сервиса полностью сфокусирована на оркестрации доступа к данным,
 * делегируя всю логику преобразования (маппинга) специализированному
 * компоненту {@link ChatHistoryMapper}. Это соответствует Принципу
 * единственной ответственности (SRP) и значительно улучшает тестируемость.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatHistoryMapper chatHistoryMapper;

    /**
     * Синхронно и транзакционно сохраняет одно сообщение в базу данных.
     *
     * @param sessionId ID сессии чата.
     * @param role      Роль отправителя сообщения.
     * @param content   Текст сообщения.
     */
    @Transactional
    public void saveMessage(UUID sessionId, MessageRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(message);
        log.debug("Сохранено сообщение для сессии {}: Role={}", sessionId, role);
    }

    /**
     * Синхронно и транзакционно загружает N последних сообщений для указанной сессии.
     * <p>
     * Результат возвращается в хронологическом порядке (от старых к новым),
     * что является корректным форматом для передачи в Spring AI Prompt.
     *
     * @param sessionId ID сессии чата.
     * @param lastN     Количество последних сообщений для загрузки.
     * @return Список сообщений {@link Message}, готовый к использованию в промпте.
     */
    @Transactional(readOnly = true)
    public List<Message> getLastNMessages(UUID sessionId, int lastN) {
        if (lastN <= 0) {
            return List.of();
        }
        PageRequest pageRequest = PageRequest.of(0, lastN, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ChatMessage> recentMessages = chatMessageRepository.findBySessionId(sessionId, pageRequest);
        log.debug("Загружено {} сообщений для сессии {}", recentMessages.size(), sessionId);

        return chatHistoryMapper.toSpringAiMessages(recentMessages);
    }
}
