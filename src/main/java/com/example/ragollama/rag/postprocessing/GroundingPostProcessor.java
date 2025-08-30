package com.example.ragollama.rag.postprocessing;

import com.example.ragollama.monitoring.GroundingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Постпроцессор, отвечающий за асинхронную проверку ответа на "обоснованность" (grounding).
 * <p>
 * Выполняется с вероятностью 10% для экономии ресурсов. Делегирует
 * асинхронное выполнение непосредственно {@link GroundingService}.
 */
@Component
@Order(30)
@RequiredArgsConstructor
public class GroundingPostProcessor implements RagPostProcessor {

    private final GroundingService groundingService;

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> process(RagProcessingContext context) {
        groundingService.verify(context.prompt().getContents(), context.response().answer());
        return CompletableFuture.completedFuture(null);
    }
}
