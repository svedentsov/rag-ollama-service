package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.testanalysis.model.HierarchicalChecklist;
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

import java.util.Map;

/**
 * AI-агент, который строит комплексные, иерархические чек-листы.
 * <p>
 * Агрегирует результаты работы других аналитических агентов и использует их
 * как контекст для генерации многоуровневого чек-листа.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChecklistBuilderAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "checklist-builder";
    }

    @Override
    public String getDescription() {
        return "Строит комплексный, иерархический чек-лист на основе анализа изменений и цели.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("goal") && context.payload().containsKey("changedFiles");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String goal = (String) context.payload().get("goal");
        try {
            Map<String, Object> contextForPrompt = new java.util.HashMap<>(context.payload());
            contextForPrompt.remove("buildFileContent");

            String analysisJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextForPrompt);
            String promptString = promptService.render("checklistBuilderPrompt", Map.of(
                    "goal", goal,
                    "analysis_results_json", analysisJson
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .map(this::parseLlmResponse)
                    .map(checklist -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Комплексный чек-лист успешно сгенерирован.",
                            Map.of("checklist", formatChecklistAsMarkdown(checklist))
                    ));

        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации контекста для Checklist Builder", e));
        }
    }

    private HierarchicalChecklist parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, HierarchicalChecklist.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от Checklist Builder LLM: {}", jsonResponse, e);
            throw new ProcessingException("Checklist Builder LLM вернул невалидный JSON.", e);
        }
    }

    private String formatChecklistAsMarkdown(HierarchicalChecklist checklist) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(checklist.title()).append("\n\n");
        sb.append(checklist.summary()).append("\n\n");

        checklist.sections().forEach((title, items) -> {
            sb.append("## ✅ ").append(title).append("\n");
            for (String item : items) {
                sb.append("- [ ] ").append(item).append("\n");
            }
            sb.append("\n");
        });

        return sb.toString();
    }
}
