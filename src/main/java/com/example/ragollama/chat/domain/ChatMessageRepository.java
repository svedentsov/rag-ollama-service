package com.example.ragollama.chat.domain;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.shared.aop.ResilientDatabaseOperation;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;

/**
 * Реактивный репозиторий для управления сущностями {@link ChatMessage}.
 * <p>
 * В эту версию добавлены методы для поиска дочерних сообщений и для
 * пакетного удаления, что необходимо для реализации каскадного удаления
 * на уровне приложения.
 */
@Repository
public interface ChatMessageRepository extends ReactiveCrudRepository<ChatMessage, UUID> {

    /**
     * Находит последние N сообщений в сессии, отсортированных по времени создания.
     *
     * @param sessionId ID сессии.
     * @param limit     Максимальное количество возвращаемых сообщений.
     * @return Реактивный поток {@link Flux} с найденными сообщениями.
     */
    @ResilientDatabaseOperation
    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY created_at DESC LIMIT :limit")
    Flux<ChatMessage> findRecentMessages(UUID sessionId, int limit);

    /**
     * Находит самое последнее сообщение в указанной сессии.
     *
     * @param sessionId ID сессии.
     * @return {@link Mono} с последним сообщением или пустой, если сообщений нет.
     */
    @ResilientDatabaseOperation
    Mono<ChatMessage> findTopBySessionIdOrderByCreatedAtDesc(UUID sessionId);

    /**
     * Находит все прямые дочерние сообщения для заданного родительского ID.
     *
     * @param parentId ID родительского сообщения.
     * @return {@link Flux} с дочерними сообщениями.
     */
    @ResilientDatabaseOperation
    Flux<ChatMessage> findByParentId(UUID parentId);

    /**
     * Удаляет все сообщения, идентификаторы которых находятся в предоставленной коллекции.
     *
     * @param ids Коллекция ID сообщений для удаления.
     * @return {@link Mono<Void>}, который завершается после выполнения операции.
     */
    @ResilientDatabaseOperation
    Mono<Void> deleteAllByIdIn(Collection<UUID> ids);

    @Override
    @ResilientDatabaseOperation
    <S extends ChatMessage> Mono<S> save(S entity);

    @Override
    @ResilientDatabaseOperation
    Mono<ChatMessage> findById(UUID uuid);
}
