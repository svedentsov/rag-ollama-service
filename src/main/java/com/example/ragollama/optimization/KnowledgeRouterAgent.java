package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.config.KnowledgeDomainProperties;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI-агент, выполняющий роль "маршрутизатора знаний".
 * <p>
 * Его задача — проанализировать запрос пользователя и, на основе
 * каталога доступных доменов знаний, выбрать наиболее подходящий
 * для поиска ответа.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeRouterAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final KnowledgeDomainProperties domainProperties;
    private final JsonExtractorUtil jsonExtractorUtil;

    private record SelectedDomains(List<String> domains) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "knowledge-router-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует запрос и выбирает наиболее релевантный домен(ы) знаний для поиска.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("query");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String query = (String) context.payload().get("query");
        String domainsForPrompt = domainProperties.domains().stream()
                .map(d -> String.format("- %s: %s", d.name(), d.description()))
                .collect(Collectors.joining("\n"));

        String promptString = promptService.render("knowledgeRouterPrompt", Map.of(
                "query", query,
                "domains", domainsForPrompt
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.FAST_RELIABLE)
                .map(this::parseLlmResponse)
                .map(selectedDomains -> {
                    log.info("Маршрутизатор выбрал домены {} для запроса: '{}'", selectedDomains.domains(), query);
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Запрос успешно маршрутизирован.",
                            Map.of("selectedDomains", selectedDomains.domains())
                    );
                });
    }

    private SelectedDomains parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, SelectedDomains.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("KnowledgeRouterAgent LLM вернул невалидный JSON.", e);
        }
    }
}
