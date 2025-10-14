package com.example.ragollama.agent.buganalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.buganalysis.model.BugReportSummary;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * QA-агент, который генерирует исполняемый скрипт для воспроизведения бага.
 * <p>
 * Этот агент принимает на вход *структурированное* описание бага
 * (полученное от {@link BugReportSummarizerAgent}) и использует LLM для
 * генерации кода API-теста, который должен падать, подтверждая наличие бага.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BugReproScriptGeneratorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "bug-repro-script-generator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Генерирует Java/RestAssured код API-теста для воспроизведения бага.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().get("bugReportSummary") instanceof BugReportSummary;
    }

    /**
     * Асинхронно выполняет генерацию кода теста.
     *
     * @param context Контекст, содержащий структурированный {@link BugReportSummary}.
     * @return {@link Mono} с результатом, содержащим сгенерированный Java-код.
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        BugReportSummary summary = (BugReportSummary) context.payload().get("bugReportSummary");
        log.info("BugReproScriptGeneratorAgent: запуск генерации скрипта для '{}'", summary.title());

        String promptString = promptService.render("bugReproScriptGeneratorPrompt", Map.of("summary", summary));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .map(tuple -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Скрипт для воспроизведения бага успешно сгенерирован.",
                        Map.of(
                                "generatedScript", tuple.getT1(),
                                "language", "java"
                        )
                ));
    }
}
