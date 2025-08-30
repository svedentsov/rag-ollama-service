package com.example.ragollama.rag.postprocessing;

import com.example.ragollama.monitoring.AuditLoggingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Постпроцессор, отвечающий за асинхронное сохранение аудиторского следа.
 * <p>
 * Эта версия захватывает `requestId` из MDC в вызывающем потоке и передает
 * его как явный параметр в асинхронный сервис, что гарантирует надежную трассировку.
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
        final String requestId = MDC.get("requestId");
        return auditLoggingService.logInteractionAsync(
                requestId,
                context.sessionId(),
                context.originalQuery(),
                context.response().sourceCitations(),
                context.prompt().getContents(),
                context.response().answer()
        );
    }
}
