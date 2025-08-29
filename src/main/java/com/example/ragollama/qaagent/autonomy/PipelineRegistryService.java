package com.example.ragollama.qaagent.autonomy;

import com.example.ragollama.qaagent.AgentOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Сервис-реестр, который предоставляет информацию о доступных
 * высокоуровневых возможностях системы (статических конвейерах).
 * <p>
 * Он динамически собирает информацию из {@link AgentOrchestratorService}
 * и форматирует ее для передачи в LLM-планировщик, выступая в роли
 * "каталога инструментов" для мета-агента.
 */
@Service
@RequiredArgsConstructor
public class PipelineRegistryService {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Возвращает каталог всех доступных статических конвейеров в виде
     * простого текста для использования в промпте.
     *
     * @return Строка с описанием доступных конвейеров, где каждый
     * конвейер представлен в виде элемента списка.
     */
    public String getCapabilitiesCatalog() {
        return orchestratorService.getAvailablePipelines().stream()
                .map(pipelineName -> "- `" + pipelineName + "`")
                .collect(Collectors.joining("\n"));
    }
}
