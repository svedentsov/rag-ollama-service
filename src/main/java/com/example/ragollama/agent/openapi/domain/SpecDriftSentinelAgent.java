package com.example.ragollama.agent.openapi.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.openapi.model.EndpointInfo;
import com.example.ragollama.agent.openapi.model.SpecDrift;
import com.example.ragollama.agent.openapi.tool.SpringEndpointInspector;
import com.example.ragollama.agent.openapi.tools.OpenApiSpecParser;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;

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
    public Mono<AgentResult> execute(AgentContext context) {
        return Mono.fromCallable(() -> {
            OpenAPI openAPI = getOpenApi(context);
            Set<EndpointInfo> specEndpoints = new HashSet<>(specParser.extractEndpoints(openAPI));
            Set<EndpointInfo> codeEndpoints = new HashSet<>(endpointInspector.getImplementedEndpoints());

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

    private OpenAPI getOpenApi(AgentContext context) {
        String specUrl = (String) context.payload().get("specUrl");
        if (specUrl != null) {
            return specParser.parseFromUrl(specUrl);
        }
        String specContent = (String) context.payload().get("specContent");
        return specParser.parseFromContent(specContent);
    }
}
