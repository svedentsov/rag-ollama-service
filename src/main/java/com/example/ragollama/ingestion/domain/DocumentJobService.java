package com.example.ragollama.ingestion.domain;

import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.ingestion.domain.model.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления жизненным циклом задач, адаптированный для R2DBC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentJobService {

    private final DocumentJobRepository jobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<List<DocumentJob>> claimNextPendingJobBatch(int batchSize) {
        return jobRepository.findByStatusOrderByCreatedAt(JobStatus.PENDING, PageRequest.of(0, batchSize))
                .collectList()
                .flatMap(pendingJobs -> {
                    if (pendingJobs.isEmpty()) {
                        return Mono.just(List.of());
                    }
                    List<UUID> jobIds = pendingJobs.stream().map(DocumentJob::getId).toList();
                    log.info("Захват пакета из {} задач. IDs: {}", pendingJobs.size(), jobIds);
                    return jobRepository.updateStatusForIds(jobIds, JobStatus.PROCESSING)
                            .then(jobRepository.findAllById(jobIds).collectList());
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> markBatchAsCompleted(List<UUID> jobIds) {
        if (jobIds.isEmpty()) {
            return Mono.empty();
        }
        return jobRepository.updateStatusForIds(jobIds, JobStatus.COMPLETED)
                .doOnSuccess(count -> log.info("Пакет из {} задач успешно завершен.", jobIds.size()))
                .then();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> markAsFailed(UUID jobId, String message) {
        return jobRepository.findById(jobId)
                .doOnNext(job -> {
                    log.error("Задача {} провалена. Причина: {}", jobId, message);
                    job.markAsFailed(message);
                })
                .flatMap(jobRepository::save)
                .then();
    }
}
