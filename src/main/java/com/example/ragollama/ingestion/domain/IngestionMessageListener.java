package com.example.ragollama.ingestion.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionMessageListener {

    private final DocumentProcessingWorker documentProcessingWorker;
    private final AgentOrchestratorService orchestratorService; // Новая зависимость

    @RabbitListener(queues = RabbitMqConfig.DOCUMENT_INGESTION_QUEUE)
    public void onDocumentIngestionRequested(UUID jobId) {
        log.info("Получена задача на индексацию из RabbitMQ. Job ID: {}. Делегирование в асинхронный воркер.", jobId);
        documentProcessingWorker.processDocument(jobId).thenRun(() -> {
            log.info("Индексация для Job ID {} завершена. Запуск CI/CD конвейера для базы знаний...", jobId);
            AgentContext context = new AgentContext(Map.of("triggeringSourceId", jobId.toString()));
            orchestratorService.invokePipeline("knowledge-ci-cd-pipeline", context)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("CI/CD конвейер для базы знаний завершился с ошибкой.", ex);
                        } else {
                            log.info("CI/CD конвейер для базы знаний успешно завершен.");
                        }
                    });
        });
    }
}
