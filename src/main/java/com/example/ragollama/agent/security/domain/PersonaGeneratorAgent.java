package com.example.ragollama.agent.security.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.security.model.AttackPersonas;
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

/**
 * AI-агент, который синтезирует "персоны" для тестирования
 * на основе извлеченных правил контроля доступа.
 * <p>
 * Этот компонент является эталоном реализации принципа инверсии зависимостей:
 * все необходимые сервисы (LLM-клиент, парсеры) внедряются через конструктор,
 * что обеспечивает максимальную тестируемость и соответствие парадигме Spring.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonaGeneratorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "persona-generator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Генерирует набор атакующих и легитимных персон на основе правил RBAC.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("extractedRules");
    }

    /**
     * {@inheritDoc}
     *
     * @param context Контекст, содержащий `extractedRules`.
     * @return {@link Mono} с результатом, содержащим сгенерированные {@link AttackPersonas}.
     * @throws ProcessingException если происходит ошибка сериализации данных для LLM.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        List<Map<String, String>> rbacRules = (List<Map<String, String>>) context.payload().get("extractedRules");
        if (rbacRules.isEmpty()) {
            return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Правила RBAC не найдены, генерация персон пропущена.", Map.of()));
        }

        try {
            String rbacJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rbacRules);
            String promptString = promptService.render("personaGeneratorPrompt", Map.of("rbac_rules_json", rbacJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .map(this::parseLlmResponse)
                    .map(attackPersonas -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Персоны для атаки успешно сгенерированы.",
                            Map.of("attackPersonas", attackPersonas)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации правил RBAC", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в строго типизированный DTO.
     *
     * @param jsonResponse Сырой строковый ответ от LLM.
     * @return Объект {@link AttackPersonas}.
     * @throws ProcessingException если LLM вернула невалидный JSON.
     */
    private AttackPersonas parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, AttackPersonas.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от Persona Generator LLM: {}", jsonResponse, e);
            throw new ProcessingException("Persona Generator LLM вернул невалидный JSON.", e);
        }
    }
}
