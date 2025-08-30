package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.git.tools.GitApiClient;
import com.example.ragollama.agent.knowledgegraph.model.CodeAnalysisResult;
import com.example.ragollama.agent.testanalysis.model.GeneratedTestFile;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который автоматически генерирует unit-тесты для нового
 * или измененного производственного кода.
 * <p>
 * Выступает в роли "парного программиста", который берет на себя рутинную
 * задачу по написанию базовых тестов, ускоряя разработку и обеспечивая
 * базовый уровень тестового покрытия.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestGeneratorAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;

    @Override
    public String getName() {
        return "test-generator";
    }

    @Override
    public String getDescription() {
        return "Генерирует JUnit 5/Mockito тесты для измененных публичных методов в Java-классе.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("codeAnalysis") && context.payload().containsKey("ref");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        CodeAnalysisResult codeAnalysis = (CodeAnalysisResult) context.payload().get("codeAnalysis");
        String ref = (String) context.payload().get("ref");
        String filePath = codeAnalysis.filePath();

        // Получаем полный код класса для контекста
        return gitApiClient.getFileContent(filePath, ref)
                .flatMapMany(fullCode -> Flux.fromIterable(codeAnalysis.methods())
                        // Генерируем тест для каждого измененного метода
                        .flatMap(method -> generateTestForMethod(fullCode, method.methodName()))
                )
                .collectList()
                .map(generatedTests -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Генерация тестов завершена. Создано " + generatedTests.size() + " файлов.",
                        Map.of("generatedTestFiles", generatedTests)
                ))
                .toFuture();
    }

    private Mono<GeneratedTestFile> generateTestForMethod(String fullClassCode, String methodName) {
        String promptString = promptService.render("testGenerator", Map.of(
                "classCode", fullClassCode,
                "methodName", methodName
        ));

        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                .map(generatedCode -> {
                    String className = fullClassCode.substring(fullClassCode.indexOf("class ") + 6, fullClassCode.indexOf("{")).trim();
                    String testFileName = className + "Test.java";
                    return new GeneratedTestFile(testFileName, generatedCode);
                });
    }
}
