package com.example.ragollama.monitoring.domain;

import com.example.ragollama.monitoring.model.FeedbackLog;
import com.example.ragollama.monitoring.model.RagAuditLog;
import com.example.ragollama.monitoring.model.TrainingDataPair;
import com.example.ragollama.rag.domain.model.SourceCitation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Асинхронный сервис ("тренер"), отвечающий за генерацию обучающих данных
 * на основе полученной обратной связи от пользователя.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackHandlerService {

    private final RagAuditLogRepository auditLogRepository;
    private final TrainingDataRepository trainingDataRepository;

    /**
     * Асинхронно генерирует и сохраняет обучающие пары (запрос, документ).
     *
     * @param feedbackLog Запись об обратной связи, которая инициировала процесс.
     */
    @Async("applicationTaskExecutor")
    @Transactional
    public void generateTrainingDataFromFeedback(FeedbackLog feedbackLog) {
        RagAuditLog auditLog = auditLogRepository.findByRequestId(feedbackLog.getRequestId())
                .orElse(null);

        if (auditLog == null || auditLog.getSourceCitations() == null || auditLog.getSourceCitations().isEmpty()) {
            log.warn("Не найден аудит или контекст для requestId {}. Обучающие данные не будут сгенерированы.", feedbackLog.getRequestId());
            return;
        }

        String query = auditLog.getOriginalQuery();
        String label = feedbackLog.getIsHelpful() ? "positive" : "negative";
        int createdPairs = 0;

        for (SourceCitation citation : Optional.ofNullable(auditLog.getSourceCitations()).orElse(Collections.emptyList())) {
            try {
                // ID чанка хранится в виде {documentId}:{chunkIndex}
                String documentIdStr = citation.chunkId().split(":")[0];
                UUID documentId = UUID.fromString(documentIdStr);

                TrainingDataPair trainingPair = TrainingDataPair.builder()
                        .queryText(query)
                        .documentId(documentId)
                        .label(label)
                        .sourceFeedback(feedbackLog)
                        .build();
                trainingDataRepository.save(trainingPair);
                createdPairs++;
            } catch (Exception e) {
                log.error("Не удалось распарсить documentId из chunkId '{}' для requestId {}",
                        citation.chunkId(), feedbackLog.getRequestId(), e);
            }
        }

        log.info("Сгенерировано {} обучающих пар(ы) с меткой '{}' для requestId {}",
                createdPairs, label, feedbackLog.getRequestId());
    }
}
