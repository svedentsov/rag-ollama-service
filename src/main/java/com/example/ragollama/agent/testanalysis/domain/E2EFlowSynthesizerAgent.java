package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.rag.domain.TestCaseService;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * QA-агент, который синтезирует сквозной (E2E) тест на основе
 * высокоуровневого описания пользовательского сценария.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class E2EFlowSynthesizerAgent implements ToolAgent {

    private final TestCaseService testCaseService;
    private final LlmClient llmClient;
    private final PromptService promptService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "e2e-flow-synthesizer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Синтезирует Java/Playwright/RestAssured E2E-тест из описания пользовательского сценария.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("userStory");
    }

    /**
     * Асинхронно выполняет синтез E2E-теста.
     *
     * @param context Контекст, содержащий описание пользовательского сценария.
     * @return {@link CompletableFuture} с результатом, содержащим сгенерированный код.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String userStory = (String) context.payload().get("userStory");
        log.info("E2EFlowSynthesizerAgent: запуск синтеза для сценария: '{}'", userStory);

        // Шаг 1: Используем RAG для сбора релевантных API-контрактов и примеров
        return findRelevantContext(userStory)
                .flatMap(contextDocs -> {
                    // Шаг 2: Передаем сценарий и контекст в LLM для генерации кода
                    String contextAsString = contextDocs.stream()
                            .map(Document::getText)
                            .collect(Collectors.joining("\n---\n"));

                    String promptString = promptService.render("e2eFlowSynthesizer", Map.of(
                            "user_story", userStory,
                            "context", contextAsString.isBlank() ? "Контекст не найден." : contextAsString
                    ));

                    return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED));
                })
                .map(generatedCode -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "E2E-тест успешно синтезирован.",
                        Map.of("generatedE2ETest", generatedCode)
                ))
                .toFuture();
    }

    /**
     * Выполняет поиск релевантных документов (API-спецификаций, существующих тестов)
     * для предоставления контекста LLM.
     *
     * @param userStory Описание пользовательского сценария.
     * @return {@link Mono} со списком релевантных документов.
     */
    private Mono<List<Document>> findRelevantContext(String userStory) {
        return testCaseService.findRelevantTestCases(userStory);
    }
}
