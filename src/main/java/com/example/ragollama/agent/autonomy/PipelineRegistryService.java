package com.example.ragollama.agent.autonomy;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentPipeline;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Сервис-реестр, который предоставляет информацию о доступных
 * высокоуровневых возможностях системы (статических конвейерах).
 * <p>
 * Он динамически собирает информацию из {@link AgentOrchestratorService}, находя
 * все зарегистрированные реализации {@link AgentPipeline}, и форматирует ее для
 * передачи в LLM-планировщик. Этот сервис выступает в роли
 * "каталога бизнес-процессов" для мета-агента {@link SdlcOrchestratorAgent}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineRegistryService {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Выводит информацию о доступных конвейерах при старте приложения.
     */
    @PostConstruct
    public void init() {
        log.info("PipelineRegistryService инициализирован. Доступные конвейеры: {}", orchestratorService.getAvailablePipelines());
    }

    /**
     * Возвращает каталог всех доступных статических конвейеров в виде
     * простого текста для использования в промпте.
     *
     * @return Строка с описанием доступных конвейеров, где каждый
     *         конвейер представлен в виде элемента списка.
     */
    public String getCapabilitiesCatalog() {
        return orchestratorService.getAvailablePipelines().stream()
                .map(pipelineName -> "- `" + pipelineName + "`")
                .collect(Collectors.joining("\n"));
    }
}
