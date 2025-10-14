package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.api.dto.ChatMessageDto;
import com.example.ragollama.chat.domain.model.ChatSession;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.monitoring.domain.RagAuditLogRepository;
import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.exception.AccessDeniedException;
import com.example.ragollama.shared.exception.ResourceNotFoundException;
import com.example.ragollama.shared.task.TaskLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Доменный сервис для управления жизненным циклом сессий чата, адаптированный для R2DBC.
 * <p>
 * Отвечает за создание, поиск, обновление и удаление сессий, а также за
 * проверку прав доступа текущего пользователя к этим сессиям.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RagAuditLogRepository ragAuditLogRepository;
    private final AppProperties appProperties;
    private final TaskLifecycleService taskLifecycleService;

    /**
     * Получает все сессии чата для текущего пользователя, обогащая их
     * информацией о последнем сообщении.
     *
     * @return {@link Flux} с DTO сессий чата.
     */
    @Transactional(readOnly = true)
    public Flux<ChatSession> getChatsForCurrentUser() {
        return chatSessionRepository.findByUserNameOrderByUpdatedAtDesc(getCurrentUsername())
                .flatMap(session ->
                        chatMessageRepository.findTopBySessionIdOrderByCreatedAtDesc(session.getSessionId())
                                .doOnNext(session::setLastMessage)
                                .thenReturn(session)
                                .defaultIfEmpty(session)
                );
    }

    /**
     * Создает новую сессию чата для текущего пользователя.
     *
     * @return {@link Mono} с созданной сущностью {@link ChatSession}.
     */
    public Mono<ChatSession> createNewChat() {
        String username = getCurrentUsername();
        String defaultName = "Новый чат от " + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        ChatSession newSession = ChatSession.builder()
                .userName(username)
                .chatName(defaultName)
                .build();
        return chatSessionRepository.save(newSession);
    }

    /**
     * Находит существующую сессию по ID или создает новую, если ID не предоставлен.
     *
     * @param sessionId Опциональный ID сессии.
     * @return {@link Mono} с найденной или созданной сессией.
     */
    public Mono<ChatSession> findOrCreateSession(UUID sessionId) {
        if (sessionId == null) {
            return createNewChat();
        }
        return findAndVerifyOwnership(sessionId);
    }

    /**
     * Обновляет имя сессии чата после проверки прав доступа.
     *
     * @param sessionId ID сессии.
     * @param newName   Новое имя.
     * @return {@link Mono}, завершающийся после сохранения.
     */
    public Mono<Void> updateChatName(UUID sessionId, String newName) {
        return findAndVerifyOwnership(sessionId)
                .flatMap(session -> {
                    session.setChatName(newName);
                    return chatSessionRepository.save(session);
                })
                .then();
    }

    /**
     * Устанавливает активную ветку для родительского сообщения в сессии.
     *
     * @param sessionId       ID сессии.
     * @param parentMessageId ID родительского сообщения.
     * @param activeChildId   ID выбранного дочернего сообщения.
     * @return {@link Mono}, завершающийся после сохранения.
     */
    public Mono<Void> setActiveBranch(UUID sessionId, UUID parentMessageId, UUID activeChildId) {
        return findAndVerifyOwnership(sessionId)
                .flatMap(session -> {
                    session.getActiveBranches().put(parentMessageId.toString(), activeChildId.toString());
                    return chatSessionRepository.save(session);
                })
                .doOnSuccess(s -> log.debug("В сессии {} для родителя {} выбрана активная ветка {}", sessionId, parentMessageId, activeChildId))
                .then();
    }

    /**
     * Удаляет сессию чата и все связанные с ней сообщения.
     *
     * @param sessionId ID сессии для удаления.
     * @return {@link Mono}, завершающийся после удаления.
     */
    public Mono<Void> deleteChat(UUID sessionId) {
        return findAndVerifyOwnership(sessionId)
                .flatMap(session ->
                        taskLifecycleService.getActiveTaskForSession(sessionId)
                                .doOnNext(task -> {
                                    log.warn("Чат {} удаляется, отменяем связанную задачу {}.", sessionId, task.getId());
                                    taskLifecycleService.cancel(task.getId()).subscribe();
                                })
                                .then(chatSessionRepository.delete(session))
                                .doOnSuccess(v -> log.info("Чат {} и все связанные с ним сообщения были успешно удалены базой данных.", sessionId))
                );
    }

    /**
     * Получает историю сообщений для указанной сессии, обогащая RAG-ответами.
     *
     * @param sessionId ID сессии.
     * @return {@link Flux} обогащенных DTO сообщений.
     */
    @Transactional(readOnly = true)
    public Flux<ChatMessageDto> getMessagesForSession(UUID sessionId) {
        return findAndVerifyOwnership(sessionId)
                .thenMany(Flux.defer(() -> {
                    int historySize = appProperties.chat().history().maxMessages();
                    return chatMessageRepository.findRecentMessages(sessionId, historySize)
                            .collectList()
                            .flatMapMany(list -> {
                                java.util.Collections.reverse(list); // findRecentMessages возвращает в DESC порядке
                                return Flux.fromIterable(list);
                            });
                }))
                .flatMap(message -> {
                    // Если это сообщение ассистента и у него есть taskId, пытаемся обогатить его
                    if (message.getRole() == MessageRole.ASSISTANT && message.getTaskId() != null) {
                        return ragAuditLogRepository.findByTaskId(message.getTaskId())
                                .map(auditLog -> ChatMessageDto.fromEntityWithAudit(message, auditLog))
                                .defaultIfEmpty(ChatMessageDto.fromEntity(message)); // Fallback, если аудит не найден
                    }
                    // Для пользовательских сообщений просто преобразуем в DTO
                    return Mono.just(ChatMessageDto.fromEntity(message));
                });
    }

    /**
     * Находит сессию по ID и проверяет, что она принадлежит текущему пользователю.
     *
     * @param sessionId ID сессии.
     * @return {@link Mono} с сущностью сессии.
     */
    public Mono<ChatSession> findAndVerifyOwnership(UUID sessionId) {
        return chatSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Чат с ID " + sessionId + " не найден.")))
                .handle((session, sink) -> {
                    if (!session.getUserName().equals(getCurrentUsername())) {
                        sink.error(new AccessDeniedException("У вас нет доступа к этому чату."));
                    } else {
                        sink.next(session);
                    }
                });
    }

    private String getCurrentUsername() {
        return "default-user";
    }
}
