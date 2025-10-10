package com.example.ragollama.chat.domain;

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
 * которая обеспечивает целостность данных при удалении сообщений, имеющих потомков.
 */
@Slf4j
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
     * @return {@link Mono}, завершающийся после сохранения.
     */
    public Mono<Void> updateMessage(UUID messageId, String newContent) {
        // Используем findByIdWithLock из chatSessionRepository для блокировки на уровне сессии
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
     * Рекурсивно удаляет сообщение и всех его потомков.
     * <p>
     * Сначала проверяются права доступа к исходному сообщению. Затем рекурсивно
     * находятся все дочерние сообщения, их ID собираются в `Set` и удаляются
     * одной пакетной операцией для максимальной производительности.
     *
     * @param messageId ID сообщения для удаления.
     * @return {@link Mono<Void>}, завершающийся после полного удаления всего дерева сообщений.
     */
    public Mono<Void> deleteMessage(UUID messageId) {
        return chatMessageRepository.findById(messageId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Сообщение с ID " + messageId + " не найдено.")))
                .flatMap(message ->
                        // 1. Проверяем права доступа
                        chatSessionService.findAndVerifyOwnership(message.getSessionId())
                                .thenReturn(message)
                )
                .flatMap(rootMessage -> {
                    // 2. Рекурсивно собираем ID всех потомков
                    Set<UUID> idsToDelete = new HashSet<>();
                    idsToDelete.add(rootMessage.getId());
                    return collectAllChildrenIds(rootMessage.getId(), idsToDelete)
                            .then(Mono.just(idsToDelete));
                })
                .flatMap(ids -> {
                    log.info("Запрос на удаление дерева сообщений. Количество: {}. IDs: {}", ids.size(), ids);
                    // 3. Удаляем все сообщения одной пакетной операцией
                    return chatMessageRepository.deleteAllByIdIn(ids);
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
                .flatMap(child -> {
                    if (collectedIds.add(child.getId())) {
                        return Flux.just(child.getId());
                    }
                    return Flux.empty();
                })
                .expand(childId ->
                        chatMessageRepository.findByParentId(childId)
                                .flatMap(grandchild -> {
                                    if (collectedIds.add(grandchild.getId())) {
                                        return Flux.just(grandchild.getId());
                                    }
                                    return Flux.empty();
                                })
                )
                .then();
    }
}
