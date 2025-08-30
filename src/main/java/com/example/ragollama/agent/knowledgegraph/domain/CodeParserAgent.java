package com.example.ragollama.agent.knowledgegraph.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.git.tools.GitApiClient;
import com.example.ragollama.agent.knowledgegraph.model.CodeAnalysisResult;
import com.example.ragollama.agent.knowledgegraph.model.LastCommitInfo;
import com.example.ragollama.agent.knowledgegraph.model.MethodDetails;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который выполняет структурный анализ Java-файла.
 * <p>
 * Этот агент является "фундаментальным инструментом", который предоставляет
 * детализированные, структурированные данные о коде для других, более
 * высокоуровневых аналитических агентов. Он разбирает код с помощью
 * JavaParser для построения Abstract Syntax Tree (AST) и использует
 * JGit для получения информации `blame` для каждого метода, создавая
 * таким образом полную картину "что, где и кем" было изменено.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeParserAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "code-parser";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Парсит Java-файл, извлекает публичные методы и связывает их с последними коммитами (git blame).";
    }

    /**
     * {@inheritDoc}
     *
     * @param context Контекст, который должен содержать 'filePath' и 'ref'.
     * @return {@code true}, если все необходимые ключи присутствуют.
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("filePath") && context.payload().containsKey("ref");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Асинхронно выполняет конвейер: получает контент файла и `blame` из Git,
     * затем парсит код и агрегирует результаты в {@link CodeAnalysisResult}.
     *
     * @param context Контекст с 'filePath' и 'ref'.
     * @return {@link CompletableFuture} со структурированным результатом анализа.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String filePath = (String) context.payload().get("filePath");
        String ref = (String) context.payload().get("ref");
        log.info("CodeParserAgent: запуск анализа для файла {} в ref {}", filePath, ref);

        Mono<String> contentMono = gitApiClient.getFileContent(filePath, ref);
        Mono<BlameResult> blameMono = gitApiClient.blameFile(filePath, ref);

        return Mono.zip(contentMono, blameMono)
                .map(tuple -> {
                    String code = tuple.getT1();
                    BlameResult blameResult = tuple.getT2();
                    return parseCode(filePath, code, blameResult);
                })
                .map(analysisResult -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Структурный анализ файла " + filePath + " успешно завершен.",
                        Map.of("codeAnalysis", analysisResult)
                ))
                .toFuture();
    }

    /**
     * Выполняет основной парсинг кода и сопоставление с данными `blame`.
     *
     * @param filePath    Путь к файлу.
     * @param code        Содержимое файла.
     * @param blameResult Результат выполнения `git blame`.
     * @return Объект {@link CodeAnalysisResult} с полной информацией.
     */
    private CodeAnalysisResult parseCode(String filePath, String code, BlameResult blameResult) {
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow(() ->
                new IllegalStateException("Не удалось распарсить Java-файл: " + filePath));

        List<MethodDetails> methodDetails = cu.findAll(MethodDeclaration.class).stream()
                .filter(md -> md.getModifiers().contains(Modifier.publicModifier()))
                .map(md -> {
                    int startLine = md.getBegin().map(p -> p.line - 1).orElse(-1);
                    int endLine = md.getEnd().map(p -> p.line - 1).orElse(-1);
                    LastCommitInfo lastCommitInfo = findLastCommitForMethod(blameResult, startLine, endLine);
                    return new MethodDetails(md.getNameAsString(), startLine + 1, endLine + 1, lastCommitInfo);
                })
                .toList();

        return new CodeAnalysisResult(filePath, methodDetails);
    }

    /**
     * Находит самый "свежий" коммит, который затрагивает строки,
     * принадлежащие одному методу.
     *
     * @param blameResult Результат `git blame`.
     * @param startLine   Начальная строка метода (0-индексированная).
     * @param endLine     Конечная строка метода (0-индексированная).
     * @return Объект {@link LastCommitInfo} или {@code null}, если информация не найдена.
     */
    private LastCommitInfo findLastCommitForMethod(BlameResult blameResult, int startLine, int endLine) {
        if (blameResult == null || startLine == -1) {
            return null;
        }

        RevCommit lastCommit = null;
        for (int i = startLine; i <= endLine; i++) {
            if (i < blameResult.getResultContents().size()) {
                RevCommit currentCommit = blameResult.getSourceCommit(i);
                if (lastCommit == null || currentCommit.getCommitTime() > lastCommit.getCommitTime()) {
                    lastCommit = currentCommit;
                }
            }
        }

        if (lastCommit == null) return null;
        return new LastCommitInfo(
                lastCommit.getId().abbreviate(7).name(),
                lastCommit.getAuthorIdent().getName(),
                lastCommit.getAuthorIdent().getEmailAddress(),
                Instant.ofEpochSecond(lastCommit.getCommitTime()),
                lastCommit.getShortMessage()
        );
    }
}
