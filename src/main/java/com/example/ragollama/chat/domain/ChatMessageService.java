package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.ChatSession;
import com.example.ragollama.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Доменный сервис для управления жизненным циклом отдельных сообщений чата, адаптированный для R2DBC.
 * <p>
 * В этой версии реализована корректная логика каскадного удаления,
 * которая обеспечивает целостность данных при удалении сообщений, имеющих потомков,
 * и очищает связанные данные в родительской сессии чата.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository; // Добавлена зависимость
    private final ChatSessionService chatSessionService;

    /**
     * Обновляет текст сообщения, принадлежащего текущему пользователю.
     *
     * @param messageId  ID сообщения для обновления.
     * @param newContent Новый текст сообщения.
     * @return {@link Mono}, завершающийся после сохранения.
     */
    public Mono<Void> updateMessage(UUID messageId, String newContent) {
        return chatMessageRepository.findById(messageId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Сообщение с ID " + messageId + " не найдено.")))
                .flatMap(message ->
                        chatSessionService.findAndVerifyOwnership(message.getSessionId())
                                .thenReturn(message)
                )
                .flatMap(message -> {
                    message.setContent(newContent);
                    return chatMessageRepository.save(message);
                })
                .then();
    }

    /**
     * Рекурсивно удаляет сообщение и всех его потомков, а также очищает
     * связанные "висячие" ссылки в родительской сессии чата.
     * <p>
     * Операция выполняется в рамках одной транзакции для обеспечения
     * полной атомарности и целостности данных.
     *
     * @param messageId ID сообщения для удаления.
     * @return {@link Mono<Void>}, завершающийся после полного удаления всего дерева сообщений.
     */
    public Mono<Void> deleteMessage(UUID messageId) {
        return chatMessageRepository.findById(messageId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Сообщение с ID " + messageId + " не найдено.")))
                .flatMap(message -> chatSessionService.findAndVerifyOwnership(message.getSessionId()).thenReturn(message))
                .flatMap(rootMessage -> {
                    Set<UUID> idsToDelete = new HashSet<>();
                    idsToDelete.add(rootMessage.getId());
                    // 1. Рекурсивно собираем ID всех потомков
                    return collectAllChildrenIds(rootMessage.getId(), idsToDelete)
                            .then(Mono.zip(
                                    Mono.just(idsToDelete),
                                    chatSessionRepository.findById(rootMessage.getSessionId())
                            ));
                })
                .flatMap(tuple -> {
                    Set<UUID> idsToDelete = tuple.getT1();
                    ChatSession session = tuple.getT2();

                    // 2. Очищаем "висячие" ссылки в activeBranches
                    boolean modified = session.getActiveBranches().entrySet()
                            .removeIf(entry -> idsToDelete.contains(UUID.fromString(entry.getKey())) ||
                                    idsToDelete.contains(UUID.fromString(String.valueOf(entry.getValue()))));

                    Mono<ChatSession> saveSessionMono = modified ? chatSessionRepository.save(session) : Mono.just(session);

                    return saveSessionMono.then(
                            // 3. Удаляем все сообщения одной пакетной операцией
                            chatMessageRepository.deleteAllByIdIn(idsToDelete)
                                    .doOnSuccess(v -> log.info("Удалено {} сообщений и очищены ссылки в сессии {}.", idsToDelete.size(), session.getSessionId()))
                    );
                });
    }

    /**
     * Рекурсивно находит всех потомков для заданного родительского ID.
     * <p>
     * Использует оператор `expand` из Project Reactor для элегантной
     * реализации рекурсивного обхода в неблокирующем стиле.
     *
     * @param parentId     ID родителя, для которого ищутся потомки.
     * @param collectedIds `Set` для аккумулирования найденных ID.
     * @return {@link Mono<Void>}, который завершается, когда все потомки найдены.
     */
    private Mono<Void> collectAllChildrenIds(UUID parentId, Set<UUID> collectedIds) {
        return chatMessageRepository.findByParentId(parentId)
                .map(ChatMessage::getId)
                .expand(childId -> {
                    if (collectedIds.add(childId)) {
                        return chatMessageRepository.findByParentId(childId).map(ChatMessage::getId);
                    }
                    return Flux.empty();
                })
                .doOnNext(collectedIds::add)
                .then();
    }
}
