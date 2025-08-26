package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.EndpointInfo;
import com.example.ragollama.qaagent.model.SpecDrift;
import com.example.ragollama.qaagent.tools.OpenApiSpecParser;
import com.example.ragollama.qaagent.tools.SpringEndpointInspector;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который выступает в роли "стража спецификации".
 * <p>
 * Он сравнивает эндпоинты, определенные в OpenAPI спецификации, с эндпоинтами,
 * фактически реализованными в коде, и сообщает о любых расхождениях (дрифте).
 * Этот агент работает полностью детерминированно, без использования LLM.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpecDriftSentinelAgent implements ToolAgent {

    private final OpenApiSpecParser specParser;
    private final SpringEndpointInspector endpointInspector;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "spec-drift-sentinel";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Обнаруживает расхождения между OpenAPI спецификацией и реализованными в коде эндпоинтами.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("specUrl") || context.payload().containsKey("specContent");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            // Шаг 1: Получаем эндпоинты из спецификации
            OpenAPI openAPI = getOpenApi(context);
            Set<EndpointInfo> specEndpoints = new HashSet<>(specParser.extractEndpoints(openAPI));

            // Шаг 2: Получаем эндпоинты из кода (из рантайм-контекста Spring)
            Set<EndpointInfo> codeEndpoints = new HashSet<>(endpointInspector.getImplementedEndpoints());

            // Шаг 3: Находим расхождения с помощью операций над множествами
            Set<EndpointInfo> missingInCode = new HashSet<>(specEndpoints);
            missingInCode.removeAll(codeEndpoints);

            Set<EndpointInfo> missingInSpec = new HashSet<>(codeEndpoints);
            missingInSpec.removeAll(specEndpoints);

            List<SpecDrift> drifts = new ArrayList<>();
            missingInCode.forEach(ep -> drifts.add(new SpecDrift(ep, SpecDrift.DriftType.MISSING_IN_CODE)));
            missingInSpec.forEach(ep -> drifts.add(new SpecDrift(ep, SpecDrift.DriftType.MISSING_IN_SPEC)));

            String summary = String.format("Анализ спецификации завершен. Найдено %d расхождений: %d отсутствуют в коде, %d отсутствуют в спецификации.",
                    drifts.size(), missingInCode.size(), missingInSpec.size());
            log.info(summary);

            return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("drifts", drifts));
        });
    }

    /**
     * Определяет источник спецификации и парсит ее.
     *
     * @param context Контекст с `specUrl` или `specContent`.
     * @return Распарсенный объект {@link OpenAPI}.
     */
    private OpenAPI getOpenApi(AgentContext context) {
        String specUrl = (String) context.payload().get("specUrl");
        if (specUrl != null) {
            return specParser.parseFromUrl(specUrl);
        }
        String specContent = (String) context.payload().get("specContent");
        return specParser.parseFromContent(specContent);
    }
}
