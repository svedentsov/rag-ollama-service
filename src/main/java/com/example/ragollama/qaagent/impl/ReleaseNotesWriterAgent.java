package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.tools.GitApiClient;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который автоматически генерирует заметки о выпуске (release notes)
 * на основе истории коммитов между двумя Git-ссылками.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseNotesWriterAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;

    @Override
    public String getName() {
        return "release-notes-writer";
    }

    @Override
    public String getDescription() {
        return "Генерирует заметки о выпуске (release notes) из истории коммитов Git.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("oldRef") && context.payload().containsKey("newRef");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String oldRef = (String) context.payload().get("oldRef");
        String newRef = (String) context.payload().get("newRef");

        log.info("ReleaseNotesWriterAgent: запуск генерации заметок о выпуске между {} и {}", oldRef, newRef);

        // Шаг 1: Асинхронно получаем сообщения коммитов
        Mono<List<String>> commitsMono = gitApiClient.getCommitMessages(oldRef, newRef);

        return commitsMono.flatMap(commitMessages -> {
            if (commitMessages.isEmpty()) {
                log.warn("Не найдено коммитов между {} и {}. Генерация заметок не требуется.", oldRef, newRef);
                return Mono.just(new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Изменений между указанными ссылками не найдено.",
                        Map.of("releaseNotesMarkdown", "Нет изменений для этого выпуска.")
                ));
            }

            // Шаг 2: Формируем промпт и вызываем LLM
            String commitLog = String.join("\n", commitMessages);
            String promptString = promptService.render("releaseNotesWriter", Map.of("commitMessages", commitLog));

            return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                    .map(releaseNotes -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Заметки о выпуске успешно сгенерированы.",
                            Map.of("releaseNotesMarkdown", releaseNotes)
                    ));
        }).toFuture();
    }
}
