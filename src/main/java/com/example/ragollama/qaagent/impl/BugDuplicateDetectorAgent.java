package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.shared.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Агент для поиска дубликатов баг-репортов.
 * Использует RAG для семантического поиска похожих тикетов в базе знаний.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BugDuplicateDetectorAgent implements QaAgent {

    private final HybridRetrievalStrategy retrievalStrategy;
    private final LlmClient llmClient;

    private static final String BUG_REPORT_TEXT_KEY = "bugReportText";
    private static final PromptTemplate PROMPT_TEMPLATE = new PromptTemplate("""
            Проанализируй предоставленный "Новый баг-репорт" и список "Похожих тикетов".
            Твоя задача - определить, является ли новый баг-репорт дубликатом одного из существующих.
            Ответь ТОЛЬКО ОДНИМ СЛОВОМ: DUPLICATE, SIMILAR или UNIQUE.
            
            Новый баг-репорт:
            {new_bug_report}
            
            Похожие тикеты:
            {similar_tickets}
            """);

    @Override
    public String getName() {
        return "bug-duplicate-detector";
    }

    @Override
    public String getDescription() {
        return "Ищет семантически похожие баг-репорты и определяет дубликаты.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey(BUG_REPORT_TEXT_KEY);
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String bugReportText = (String) context.payload().get(BUG_REPORT_TEXT_KEY);

        // Шаг 1: Используем retrieval-часть RAG для поиска похожих документов (багов)
        return retrievalStrategy.retrieve(List.of(bugReportText), bugReportText)
                .toFuture()
                .thenCompose(similarDocs -> {
                    if (similarDocs.isEmpty()) {
                        return CompletableFuture.completedFuture(new AgentResult(
                                getName(),
                                AgentResult.Status.SUCCESS,
                                "Похожих баг-репортов не найдено. Вероятно, тикет уникален.",
                                Map.of("status", "UNIQUE")
                        ));
                    }

                    String similarTicketsContext = similarDocs.stream()
                            .map(doc -> "ID: " + doc.getMetadata().get("documentId") + "\n" + doc.getText())
                            .collect(Collectors.joining("\n---\n"));

                    String promptString = PROMPT_TEMPLATE.render(Map.of(
                            "new_bug_report", bugReportText,
                            "similar_tickets", similarTicketsContext
                    ));

                    // Шаг 2: Используем LLM для финального вердикта
                    return llmClient.callChat(new Prompt(promptString))
                            .thenApply(llmResponse -> {
                                String summary = "Анализ на дубликаты завершен. Статус: " + llmResponse;
                                return new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        summary,
                                        Map.of("status", llmResponse.trim(), "candidates", similarDocs)
                                );
                            });
                });
    }
}
