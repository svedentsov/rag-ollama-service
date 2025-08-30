package com.example.ragollama.rag.postprocessing;

import com.example.ragollama.monitoring.SourceCiteVerifierService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Постпроцессор, отвечающий за асинхронную верификацию цитирования источников в ответе.
 * <p>
 * Эта версия напрямую вызывает асинхронный {@link SourceCiteVerifierService}
 * и возвращает его {@link CompletableFuture}.
 */
@Component
@Order(20)
@RequiredArgsConstructor
public class VerificationPostProcessor implements RagPostProcessor {

    private final SourceCiteVerifierService verifierService;

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> process(RagProcessingContext context) {
        verifierService.verify(context.documents(), context.response().answer());
        return CompletableFuture.completedFuture(null);
    }
}
