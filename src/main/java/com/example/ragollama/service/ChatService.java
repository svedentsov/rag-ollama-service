package com.example.ragollama.service;

import com.example.ragollama.dto.ChatRequest;
import com.example.ragollama.dto.ChatResponse;
import com.example.ragollama.entity.ChatMessage;
import com.example.ragollama.entity.MessageRole;
import com.example.ragollama.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сервис для управления логикой простого чата с AI.
 * <p>
 * Отвечает за обработку запросов, взаимодействие с LLM,
 * защиту от prompt injection и сохранение истории диалога в базу данных.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final ChatMessageRepository chatMessageRepository;
    private final PromptGuardService promptGuardService;

    /**
     * Обрабатывает запрос пользователя в чат.
     * <p>
     * Процесс обработки включает:
     * 1. Проверку на prompt injection.
     * 2. Определение ID сессии (использование существующего или создание нового).
     * 3. Сохранение сообщения пользователя в БД.
     * 4. Отправку запроса к LLM через {@link ChatClient}.
     * 5. Сохранение ответа AI в БД.
     * 6. Формирование и возврат ответа клиенту.
     *
     * @param request DTO с сообщением пользователя и ID сессии.
     * @return {@link ChatResponse} с ответом AI и ID сессии.
     */
    @Transactional
    public ChatResponse processChatRequest(ChatRequest request) {
        // 1. Проверка на вредоносный ввод
        promptGuardService.checkForInjection(request.message());

        // 2. Управление сессией
        UUID sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID();
        log.info("Обработка запроса в чат для сессии ID: {}", sessionId);

        // 3. Сохранение сообщения пользователя
        saveMessage(sessionId, MessageRole.USER, request.message());

        // 4. Взаимодействие с LLM
        Prompt prompt = new Prompt(request.message());
        String aiResponseContent = chatClient.prompt(prompt)
                .call()
                .content();
        log.debug("Ответ AI для сессии {}: {}", sessionId, aiResponseContent);

        // 5. Сохранение ответа AI
        saveMessage(sessionId, MessageRole.ASSISTANT, aiResponseContent);

        // 6. Возврат результата
        return new ChatResponse(aiResponseContent, sessionId);
    }

    /**
     * Приватный метод для сохранения сообщения в базу данных.
     *
     * @param sessionId ID текущей сессии.
     * @param role      Роль отправителя (USER или ASSISTANT).
     * @param content   Текст сообщения.
     */
    private void saveMessage(UUID sessionId, MessageRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(message);
        log.debug("Сохранено сообщение для сессии {}: Role={}, Content='{}...'", sessionId, role,
                content.substring(0, Math.min(content.length(), 50)));
    }
}
