package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * QA-агент, который генерирует чек-лист для ручного тестирования
 * на основе описания функциональности.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChecklistGeneratorAgent implements ToolAgent {

    public static final String FEATURE_DESCRIPTION_KEY = "featureDescription";

    private final LlmClient llmClient;
    private final PromptService promptService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "checklist-generator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Генерирует чек-лист для ручного тестирования по описанию функциональности.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey(FEATURE_DESCRIPTION_KEY);
    }

    /**
     * Асинхронно выполняет генерацию чек-листа.
     *
     * @param context Контекст, содержащий описание функциональности.
     * @return {@link Mono} с результатом, содержащим сгенерированный чек-лист.
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String featureDescription = (String) context.payload().get(FEATURE_DESCRIPTION_KEY);
        String promptString = promptService.render("checklistGeneratorPrompt", Map.of("feature_description", featureDescription));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .map(tuple -> {
                    String llmResponse = tuple.getT1();
                    List<String> checklistItems = parseToList(llmResponse);
                    String summary = "Чек-лист успешно сгенерирован. Найдено " + checklistItems.size() + " пунктов для проверки.";
                    log.info("ChecklistGeneratorAgent: сгенерирован чек-лист из {} пунктов.", checklistItems.size());

                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            summary,
                            Map.of("checklist", checklistItems)
                    );
                });
    }

    private List<String> parseToList(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return List.of();
        }
        return Arrays.stream(llmResponse.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }
}
