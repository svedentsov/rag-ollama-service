package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.GeneratedTestFile;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Продвинутый AI-агент, который генерирует unit-тесты, обучаясь на
 * существующих примерах из кодовой базы проекта.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelfImprovingTestDesignerAgent implements ToolAgent {

    private final VectorStore vectorStore;
    private final LlmClient llmClient;
    private final PromptService promptService;

    @Override
    public String getName() {
        return "self-improving-test-designer";
    }

    @Override
    public String getDescription() {
        return "Генерирует JUnit 5 тесты, используя лучшие примеры из существующей кодовой базы.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("classCode") && context.payload().containsKey("methodName");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String classCode = (String) context.payload().get("classCode");
        String methodName = (String) context.payload().get("methodName");

        return findRelevantTestExamples(classCode, methodName)
                .flatMap(examples -> generateTest(classCode, methodName, examples))
                .map(generatedTest -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Тест успешно сгенерирован с использованием примеров из проекта.",
                        Map.of("generatedTestFile", generatedTest)
                ))
                .toFuture();
    }

    private Mono<List<Document>> findRelevantTestExamples(String classCode, String methodName) {
        String searchQuery = "unit test for method " + methodName + " in class: " + classCode;
        SearchRequest request = SearchRequest.builder()
                .query(searchQuery)
                .topK(3)
                .filterExpression("metadata.doc_type == 'unit_test'")
                .build();

        return Mono.fromCallable(() -> vectorStore.similaritySearch(request))
                .doOnSuccess(docs -> log.info("Найдено {} релевантных примеров тестов для метода {}", docs.size(), methodName));
    }

    private Mono<GeneratedTestFile> generateTest(String classCode, String methodName, List<Document> examples) {
        String examplesAsString = examples.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        String promptString = promptService.render("selfImprovingTest", Map.of(
                "classCode", classCode,
                "methodName", methodName,
                "examples", examplesAsString.isBlank() ? "Примеры не найдены." : examplesAsString
        ));
        Prompt prompt = new Prompt(new UserMessage(promptString));
        return Mono.fromFuture(llmClient.callChat(prompt, ModelCapability.BALANCED))
                .map(generatedCode -> {
                    String className = classCode.substring(classCode.indexOf("class ") + 6, classCode.indexOf("{")).trim();
                    String testFileName = className + "Test.java";
                    return new GeneratedTestFile(testFileName, generatedCode);
                });
    }
}
