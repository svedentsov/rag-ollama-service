package com.example.ragollama.evaluation.domain;

import com.example.ragollama.monitoring.domain.FeedbackLogRepository;
import com.example.ragollama.monitoring.domain.RagAuditLogRepository;
import com.example.ragollama.rag.domain.model.SourceCitation;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Доменный сервис для сбора контекста по ID фидбэка, адаптированный для R2DBC.
 */
@Service
@RequiredArgsConstructor
public class FeedbackAnalysisService {

    private final FeedbackLogRepository feedbackLogRepository;
    private final RagAuditLogRepository ragAuditLogRepository;

    @Builder
    public record FeedbackContext(
            String originalQuery,
            String badAnswer,
            String userComment,
            List<String> retrievedDocumentIds
    ) {}

    @Transactional(readOnly = true)
    public Mono<FeedbackContext> getContextForFeedback(UUID feedbackId) {
        return feedbackLogRepository.findById(feedbackId)
                .flatMap(feedbackLog -> ragAuditLogRepository.findByRequestId(feedbackLog.getRequestId())
                        .map(auditLog -> FeedbackContext.builder()
                                .originalQuery(auditLog.getOriginalQuery())
                                .badAnswer(auditLog.getLlmAnswer())
                                .userComment(feedbackLog.getUserComment())
                                .retrievedDocumentIds(
                                        Optional.ofNullable(auditLog.getSourceCitations())
                                                .orElse(Collections.emptyList())
                                                .stream()
                                                .map(SourceCitation::chunkId)
                                                .collect(Collectors.toList())
                                )
                                .build()));
    }
}
