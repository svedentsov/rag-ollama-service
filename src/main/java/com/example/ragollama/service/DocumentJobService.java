package com.example.ragollama.service;

import com.example.ragollama.entity.DocumentJob;
import com.example.ragollama.entity.JobStatus;
import com.example.ragollama.repository.DocumentJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления жизненным циклом задач по обработке документов (DocumentJob).
 * Реализует логику атомарного захвата пакетов задач и делегирует управление
 * состоянием самой сущности {@link DocumentJob}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentJobService {

    private final DocumentJobRepository jobRepository;

    /**
     * Атомарно "захватывает" пакет ожидающих задач из очереди.
     * Метод выполняется в новой транзакции ({@code REQUIRES_NEW}) для обеспечения
     * изоляции от других операций. Он находит пакет ожидающих задач и
     * немедленно обновляет их статус на PROCESSING, предотвращая их
     * захват другими экземплярами планировщика.
     *
     * @param batchSize Максимальный размер пакета задач для захвата.
     * @return Список захваченных задач. Если задач нет, возвращает пустой список.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<DocumentJob> claimNextPendingJobBatch(int batchSize) {
        List<DocumentJob> pendingJobs = jobRepository.findByStatusOrderByCreatedAt(
                JobStatus.PENDING, PageRequest.of(0, batchSize));

        if (pendingJobs.isEmpty()) {
            return List.of();
        }

        List<UUID> jobIds = pendingJobs.stream().map(DocumentJob::getId).toList();
        log.info("Захват пакета из {} задач для обработки. IDs: {}", pendingJobs.size(), jobIds);

        jobRepository.updateStatusForIds(jobIds, JobStatus.PROCESSING);

        // Перечитываем сущности, чтобы вернуть их с обновленным статусом
        return jobRepository.findAllById(jobIds);
    }

    /**
     * Помечает пакет задач как успешно завершенный.
     * Использует пакетный SQL-запрос для максимальной производительности.
     *
     * @param jobIds Список ID задач для обновления.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markBatchAsCompleted(List<UUID> jobIds) {
        if (!jobIds.isEmpty()) {
            jobRepository.updateStatusForIds(jobIds, JobStatus.COMPLETED);
            log.info("Пакет из {} задач успешно завершен.", jobIds.size());
        }
    }

    /**
     * Помечает одну задачу как проваленную, сохраняя сообщение об ошибке.
     * Этот метод находит соответствующую сущность {@link DocumentJob} и вызывает
     * ее собственный бизнес-метод {@code markAsFailed(message)}. Это гарантирует,
     * что вся логика смены состояния инкапсулирована внутри доменной модели.
     *
     * @param jobId   ID задачи.
     * @param message Сообщение об ошибке.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(UUID jobId, String message) {
        jobRepository.findById(jobId).ifPresent(job -> {
            log.error("Задача {} провалена. Причина: {}", jobId, message);
            job.markAsFailed(message);
            jobRepository.save(job);
        });
    }
}
