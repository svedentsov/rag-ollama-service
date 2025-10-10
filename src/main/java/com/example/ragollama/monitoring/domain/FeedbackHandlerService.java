package com.example.ragollama.monitoring.domain;

import com.example.ragollama.monitoring.model.FeedbackLog;
import com.example.ragollama.monitoring.model.TrainingDataPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Асинхронный сервис ("тренер") для генерации обучающих данных, адаптированный для R2DBC.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackHandlerService {

    private final RagAuditLogRepository auditLogRepository;
    private final TrainingDataRepository trainingDataRepository;

    @Async("applicationTaskExecutor")
    @Transactional
    public void generateTrainingDataFromFeedback(FeedbackLog feedbackLog) {
        auditLogRepository.findByRequestId(feedbackLog.getRequestId())
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.warn("Не найден аудит для requestId {}. Обучающие данные не будут сгенерированы.", feedbackLog.getRequestId())
                ))
                .flatMapMany(auditLog -> {
                    if (auditLog.getSourceCitations() == null || auditLog.getSourceCitations().isEmpty()) {
                        return Flux.empty();
                    }
                    String query = auditLog.getOriginalQuery();
                    String label = feedbackLog.getIsHelpful() ? "positive" : "negative";
                    return Flux.fromIterable(Optional.ofNullable(auditLog.getSourceCitations()).orElse(Collections.emptyList()))
                            .flatMap(citation -> {
                                try {
                                    String documentIdStr = citation.chunkId().split(":")[0];
                                    UUID documentId = UUID.fromString(documentIdStr);
                                    TrainingDataPair trainingPair = TrainingDataPair.builder()
                                            .queryText(query)
                                            .documentId(documentId)
                                            .label(label)
                                            .sourceFeedbackId(feedbackLog.getId())
                                            .build();
                                    return trainingDataRepository.save(trainingPair);
                                } catch (Exception e) {
                                    log.error("Не удалось обработать citation '{}' для requestId {}", citation.chunkId(), feedbackLog.getRequestId(), e);
                                    return Mono.empty();
                                }
                            });
                })
                .count()
                .subscribe(createdPairs -> log.info("Сгенерировано {} обучающих пар для requestId {}", createdPairs, feedbackLog.getRequestId()));
    }
}
