package com.example.ragollama.rag.postprocessing;

import com.example.ragollama.monitoring.AuditLoggingService.AuditLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Постпроцессор, отвечающий за асинхронное сохранение аудиторского следа.
 */
@Component
@Order(10)
@RequiredArgsConstructor
public class AuditPostProcessor implements RagPostProcessor {

    private final AuditLoggingService auditLoggingService;

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> process(RagProcessingContext context) {
        return auditLoggingService.logInteractionAsync(
                context.requestId(),
                context.sessionId(),
                context.originalQuery(),
                context.response().sourceCitations(),
                context.prompt().getContents(),
                context.response().answer()
        );
    }
}
