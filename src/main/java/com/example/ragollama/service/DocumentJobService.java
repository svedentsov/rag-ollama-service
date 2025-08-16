package com.example.ragollama.service;

import com.example.ragollama.entity.DocumentJob;
import com.example.ragollama.entity.JobStatus;
import com.example.ragollama.repository.DocumentJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для управления жизненным циклом задач по обработке документов (DocumentJob).
 * Инкапсулирует логику изменения состояния задач в коротких, атомарных транзакциях,
 * что обеспечивает отказоустойчивость и предотвращает состояния гонки
 * в распределенной среде.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentJobService {

    private final DocumentJobRepository jobRepository;

    /**
     * Атомарно "захватывает" следующую ожидающую задачу из очереди.
     * Этот метод в рамках новой, изолированной транзакции находит первую задачу
     * со статусом PENDING, меняет ее статус на PROCESSING и сохраняет. Это гарантирует,
     * что только один обработчик (worker) сможет взять эту задачу в работу.
     *
     * @return {@link Optional} с захваченной задачей или пустой, если задач в очереди нет.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<DocumentJob> claimNextPendingJob() {
        return jobRepository.findFirstByStatusOrderByCreatedAt(JobStatus.PENDING)
                .map(job -> {
                    job.setStatus(JobStatus.PROCESSING);
                    job.setErrorMessage(null); // Очищаем старые ошибки, если есть
                    log.info("Задача {} захвачена в обработку.", job.getId());
                    return jobRepository.save(job);
                });
    }

    /**
     * Помечает задачу как успешно завершенную.
     *
     * @param jobId ID задачи для обновления.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsCompleted(UUID jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.COMPLETED);
            jobRepository.save(job);
            log.info("Задача {} успешно завершена.", jobId);
        });
    }

    /**
     * Помечает задачу как проваленную, сохраняя сообщение об ошибке.
     *
     * @param jobId   ID задачи для обновления.
     * @param message Сообщение об ошибке.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(UUID jobId, String message) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(message);
            jobRepository.save(job);
            log.error("Задача {} провалена. Причина: {}", jobId, message);
        });
    }
}
