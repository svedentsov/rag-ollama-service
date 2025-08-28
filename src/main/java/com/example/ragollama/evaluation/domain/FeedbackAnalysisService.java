package com.example.ragollama.evaluation.domain;

import com.example.ragollama.monitoring.domain.FeedbackLogRepository;
import com.example.ragollama.monitoring.domain.RagAuditLogRepository;
import com.example.ragollama.monitoring.model.FeedbackLog;
import com.example.ragollama.monitoring.model.RagAuditLog;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Доменный сервис для сбора полного контекста по ID обратной связи.
 */
@Service
@RequiredArgsConstructor
public class FeedbackAnalysisService {

    private final FeedbackLogRepository feedbackLogRepository;
    private final RagAuditLogRepository ragAuditLogRepository;

    /**
     * DTO для передачи полного контекста фидбэка.
     */
    @Builder
    public record FeedbackContext(
            String originalQuery,
            String badAnswer,
            String userComment,
            List<String> retrievedDocumentIds
    ) {
    }

    /**
     * Собирает все "улики", связанные с одним фидбэком, в единый объект.
     *
     * @param feedbackId ID фидбэка.
     * @return {@link Optional} с полным контекстом.
     */
    @Transactional(readOnly = true)
    public Optional<FeedbackContext> getContextForFeedback(UUID feedbackId) {
        Optional<FeedbackLog> feedbackLogOpt = feedbackLogRepository.findById(feedbackId);
        if (feedbackLogOpt.isEmpty()) {
            return Optional.empty();
        }
        FeedbackLog feedbackLog = feedbackLogOpt.get();

        Optional<RagAuditLog> auditLogOpt = ragAuditLogRepository.findByRequestId(feedbackLog.getRequestId());
        if (auditLogOpt.isEmpty()) {
            return Optional.empty();
        }
        RagAuditLog auditLog = auditLogOpt.get();

        return Optional.of(FeedbackContext.builder()
                .originalQuery(auditLog.getOriginalQuery())
                .badAnswer(auditLog.getLlmAnswer())
                .userComment(feedbackLog.getUserComment())
                .retrievedDocumentIds(auditLog.getContextDocuments())
                .build());
    }
}
