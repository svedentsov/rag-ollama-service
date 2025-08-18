package com.example.ragollama.chat.domain;

import com.example.ragollama.shared.aop.ResilientDatabaseOperation;
import com.example.ragollama.chat.domain.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link ChatMessage}.
 * Методы этого репозитория защищены от транзиентных сбоев базы данных
 * с помощью аннотации {@link ResilientDatabaseOperation}.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Находит последние сообщения для указанной сессии, используя пагинацию.
     * <p>
     * Этот метод защищен от временных сбоев базы данных (например, проблем с
     * сетевым подключением) благодаря механизму Retry и Circuit Breaker.
     *
     * @param sessionId Идентификатор сессии чата, для которой извлекаются сообщения.
     * @param pageable  Объект, содержащий информацию о странице (номер, размер,
     *                  сортировка) для ограничения количества результатов.
     * @return Список {@link ChatMessage}, отсортированный в соответствии с параметрами {@code pageable}.
     */
    @ResilientDatabaseOperation
    List<ChatMessage> findBySessionId(UUID sessionId, Pageable pageable);

    /**
     * Сохраняет новую или обновляет существующую сущность {@link ChatMessage}.
     * <p>
     * Этот метод также защищен от временных сбоев базы данных, обеспечивая
     * надежность операций записи.
     *
     * @param entity Сущность для сохранения. Должна быть не {@code null}.
     * @param <S>    Тип сохраняемой сущности.
     * @return Сохраненная сущность; никогда не будет {@code null}.
     */
    @ResilientDatabaseOperation
    @Override
    <S extends ChatMessage> S save(S entity);
}
