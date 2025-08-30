package com.example.ragollama.shared.tool.codeanalysis;

import com.example.ragollama.shared.model.codeanalysis.CodeMetrics;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сервис-инструмент для выполнения статического анализа Java-кода с помощью PMD.
 */
@Slf4j
@Service
public class StaticCodeAnalyzerService {

    private static final Pattern NUMBER_IN_TEXT = Pattern.compile("(\\d+)");

    public CodeMetrics analyze(String javaCode) {
        if (javaCode == null || javaCode.isBlank()) {
            return new CodeMetrics(0, 0, 0);
        }
        PMDConfiguration config = new PMDConfiguration();
        var langVersion = LanguageRegistry.PMD.getLanguageVersionById("java", "21");
        if (langVersion == null) {
            var lang = LanguageRegistry.PMD.getLanguageById("java");
            if (lang != null) {
                langVersion = lang.getDefaultVersion();
                log.warn("Java 21 language version not found in PMD registry — falling back to default: {}",
                        langVersion);
            } else {
                log.error("Java language not found in PMD registry; aborting PMD analysis");
                return new CodeMetrics(0, 0, 0);
            }
        }
        config.setDefaultLanguageVersion(langVersion);

        AtomicInteger cyclomaticComplexity = new AtomicInteger(0);
        AtomicInteger nPathComplexity = new AtomicInteger(0);

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            pmd.addRuleSet(pmd.newRuleSetLoader().loadFromResource("category/java/design.xml"));
            pmd.files().addSourceFile(FileId.fromPathLikeString("virtual.java"), javaCode);
            Report report = pmd.performAnalysisAndCollectReport();
            for (RuleViolation v : report.getViolations()) {
                String ruleName = v.getRule().getName();
                String desc = v.getDescription();
                Matcher m = NUMBER_IN_TEXT.matcher(desc);
                if (m.find()) {
                    int value = Integer.parseInt(m.group(1));
                    if ("CyclomaticComplexity".equals(ruleName) || ruleName.contains("Cyclomatic")) {
                        cyclomaticComplexity.addAndGet(value);
                    } else if ("NPathComplexity".equals(ruleName) || ruleName.contains("NPath")) {
                        nPathComplexity.addAndGet(value);
                    }
                } else {
                    // Если число не найдено — считаем нарушение как единицу (опционально)
                    if ("CyclomaticComplexity".equals(ruleName) || ruleName.contains("Cyclomatic")) {
                        cyclomaticComplexity.incrementAndGet();
                    } else if ("NPathComplexity".equals(ruleName) || ruleName.contains("NPath")) {
                        nPathComplexity.incrementAndGet();
                    }
                }
            }
        } catch (Exception e) {
            log.error("PMD analysis failed", e);
            return new CodeMetrics(0, 0, 0);
        }

        int lineCount = Math.toIntExact(javaCode.lines().count());
        return new CodeMetrics(cyclomaticComplexity.get(), nPathComplexity.get(), lineCount);
    }
}
