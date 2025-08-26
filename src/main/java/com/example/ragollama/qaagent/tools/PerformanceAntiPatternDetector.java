package com.example.ragollama.qaagent.tools;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для детерминированного поиска известных анти-паттернов производительности
 * в Java-коде с использованием AST-анализа.
 */
@Service
public class PerformanceAntiPatternDetector {

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));

    /**
     * DTO для представления одного найденного анти-паттерна.
     */
    @Getter
    @RequiredArgsConstructor
    public static class AntiPatternOccurrence {
        private final String type = "DB_CALL_IN_LOOP";
        private final int startLine;
        private final int endLine;
        private final String codeSnippet;
    }

    /**
     * Обнаруживает вызовы к репозиториям внутри циклов (проблема N+1).
     *
     * @param javaCode Исходный код для анализа.
     * @return Список найденных проблем.
     */
    public List<AntiPatternOccurrence> detectDbCallsInLoops(String javaCode) {
        List<AntiPatternOccurrence> occurrences = new ArrayList<>();
        javaParser.parse(javaCode).getResult().ifPresent(cu -> {
            cu.findAll(ForStmt.class).forEach(loop -> findDbCalls(loop, occurrences));
            cu.findAll(ForEachStmt.class).forEach(loop -> findDbCalls(loop, occurrences));
            cu.findAll(WhileStmt.class).forEach(loop -> findDbCalls(loop, occurrences));
        });
        return occurrences;
    }

    private void findDbCalls(com.github.javaparser.ast.Node loop, List<AntiPatternOccurrence> occurrences) {
        loop.findAll(MethodCallExpr.class).forEach(call -> {
            boolean isRepositoryCall = call.getScope()
                    .map(scope -> scope.toString().endsWith("Repository"))
                    .orElse(false);

            boolean isDbMethod = call.getNameAsString().matches("find.*|get.*|save.*|delete.*|exists.*");

            if (isRepositoryCall && isDbMethod) {
                occurrences.add(new AntiPatternOccurrence(
                        loop.getBegin().map(p -> p.line).orElse(0),
                        loop.getEnd().map(p -> p.line).orElse(0),
                        loop.toString()
                ));
            }
        });
    }
}
