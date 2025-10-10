package com.example.ragollama.monitoring.domain;

import com.example.ragollama.monitoring.api.dto.FeedbackRequest;
import com.example.ragollama.monitoring.model.FeedbackLog;
import com.example.ragollama.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Сервис-оркестратор для обработки входящей обратной связи, адаптированный для R2DBC.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackLogRepository feedbackLogRepository;
    private final RagAuditLogRepository auditLogRepository;
    private final FeedbackHandlerService feedbackHandlerService;

    /**
     * Обрабатывает запрос на обратную связь.
     *
     * @param request DTO с данными от пользователя.
     * @return {@link Mono}, завершающийся после сохранения.
     */
    @Transactional
    public Mono<Void> processFeedback(FeedbackRequest request) {
        return auditLogRepository.existsByRequestId(request.requestId())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ResourceNotFoundException("RAG interaction with request ID " + request.requestId() + " not found."));
                    }

                    FeedbackLog feedbackLog = FeedbackLog.builder()
                            .requestId(request.requestId())
                            .isHelpful(request.isHelpful())
                            .userComment(request.comment())
                            .build();

                    return feedbackLogRepository.save(feedbackLog)
                            .doOnSuccess(savedFeedback -> {
                                log.info("Фидбэк для requestId {} сохранен. Запуск асинхронного обработчика...", request.requestId());
                                feedbackHandlerService.generateTrainingDataFromFeedback(savedFeedback);
                            });
                })
                .then();
    }
}
