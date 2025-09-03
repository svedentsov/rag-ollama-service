package com.example.ragollama.evaluation.domain;

import com.example.ragollama.monitoring.domain.FeedbackLogRepository;
import com.example.ragollama.monitoring.domain.RagAuditLogRepository;
import com.example.ragollama.rag.domain.model.SourceCitation;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Доменный сервис для сбора полного контекста по ID обратной связи.
 * <p>
 * Выполняет роль "детектива", который, имея на руках только ID фидбэка,
 * собирает все связанные с ним "улики" (исходный запрос, ответ,
 * использованные документы) из разных таблиц для передачи в
 * аналитический AI-агент.
 */
@Service
@RequiredArgsConstructor
public class FeedbackAnalysisService {

    private final FeedbackLogRepository feedbackLogRepository;
    private final RagAuditLogRepository ragAuditLogRepository;

    /**
     * DTO для инкапсуляции полного контекста, связанного с одним фидбэком.
     *
     * @param originalQuery        Исходный вопрос пользователя.
     * @param badAnswer            Неправильный ответ, сгенерированный системой.
     * @param userComment          Комментарий пользователя, объясняющий проблему.
     * @param retrievedDocumentIds Список ID документов, которые система фактически
     *                             использовала для генерации плохого ответа.
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
     * <p>
     * Выполняется в одной транзакции для обеспечения консистентности данных.
     *
     * @param feedbackId ID фидбэка, по которому нужно собрать контекст.
     * @return {@link Optional} с полным контекстом, или пустой, если
     * связанные записи не найдены.
     */
    @Transactional(readOnly = true)
    public Optional<FeedbackContext> getContextForFeedback(UUID feedbackId) {
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
