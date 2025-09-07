package com.example.ragollama.agent.buganalysis.domain;

import com.example.ragollama.agent.buganalysis.api.dto.BugAnalysisResponse;
import com.example.ragollama.agent.buganalysis.mappers.BugAnalysisMapper;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Сервис-оркестратор для агента анализа баг-репортов.
 *
 * <p>Эта версия реализует собственный, специализированный RAG-конвейер,
 * чтобы гарантировать получение от LLM строго структурированного JSON-ответа.
 * Это решает проблему падений при парсинге и делает сервис более надежным
 * и независимым от общей RAG-стратегии.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BugAnalysisService {

    private final HybridRetrievalStrategy retrievalStrategy;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final BugAnalysisMapper bugAnalysisMapper;

    /**
     * Асинхронно анализирует баг-репорт, используя специализированный конвейер.
     *
     * @param draftDescription Черновик описания бага от пользователя.
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * структурированный {@link BugAnalysisResponse}.
     */
    public CompletableFuture<BugAnalysisResponse> analyzeBugReport(String draftDescription) {
        log.info("Запуск специализированного анализа бага для: '{}'", draftDescription);

        // Шаг 1: Найти семантически похожие баг-репорты (кандидаты в дубликаты)
        return findSimilarBugReports(draftDescription)
                .flatMap(candidateDocs -> {
                    // Шаг 2: Сформировать специализированный промпт
                    String contextBugs = formatCandidatesForPrompt(candidateDocs);
                    String promptString = promptService.render("bugAnalysisPrompt", Map.of(
                            "draft_report", draftDescription,
                            "context_bugs", contextBugs
                    ));

                    // Шаг 3: Вызвать LLM с требованием вернуть JSON
                    return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED));
                })
                // Шаг 4: Распарсить гарантированно полученный JSON
                .map(bugAnalysisMapper::parse)
                .toFuture();
    }

    /**
     * Выполняет поиск по базе знаний для нахождения похожих отчетов об ошибках.
     *
     * @param description Текст для поиска.
     * @return {@link Mono} со списком документов-кандидатов.
     */
    private Mono<List<Document>> findSimilarBugReports(String description) {
        // Здесь мы можем использовать упрощенный поиск, так как нам не нужны сложные
        // трансформации запроса для этой задачи.
        return retrievalStrategy.retrieve(null, description, 5, 0.75, null);
    }

    /**
     * Форматирует найденные документы в простой текстовый вид для подстановки в промпт.
     *
     * @param documents Список документов-кандидатов.
     * @return Единая строка с контекстом.
     */
    private String formatCandidatesForPrompt(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "Похожих баг-репортов в базе знаний не найдено.";
        }
        return documents.stream()
                .map(doc -> String.format("- ID: %s\n  Текст: %s",
                        doc.getMetadata().get("source"),
                        doc.getText()))
                .collect(Collectors.joining("\n---\n"));
    }
}
