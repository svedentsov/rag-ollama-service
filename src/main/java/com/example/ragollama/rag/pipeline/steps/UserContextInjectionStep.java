package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.web.FileContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Шаг конвейера, который внедряет контекст из файлов, предоставленных пользователем.
 */
@Component
@Order(5) // Выполняется очень рано, до обработки запроса и поиска
@Slf4j
@RequiredArgsConstructor
public class UserContextInjectionStep implements RagPipelineStep {

    private final FileContentService fileContentService;

    @Override
    @SuppressWarnings("unchecked")
    public Mono<RagFlowContext> process(RagFlowContext context) {
        Object fileIdsObject = context.promptModel().get("fileIds");

        if (!(fileIdsObject instanceof List<?> fileIds && !fileIds.isEmpty())) {
            return Mono.just(context);
        }

        List<UUID> uuidList = fileIds.stream()
                .map(id -> UUID.fromString(id.toString()))
                .toList();

        log.info("Шаг [05] User Context Injection: загрузка контента для {} файлов...", uuidList.size());

        return fileContentService.getAggregatedContent(uuidList)
                .map(context::withUserProvidedContext)
                .defaultIfEmpty(context);
    }
}
