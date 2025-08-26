package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.tools.GitApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент для инспекции Git-репозитория.
 * <p>
 * Основная задача этого агента — получить список файлов, измененных
 * между двумя указанными Git-ссылками (коммитами, ветками или тегами).
 * Он делегирует всю низкоуровневую работу с Git-репозиторием
 * специализированному клиенту {@link GitApiClient}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitInspectorAgent implements ToolAgent {

    public static final String OLD_REF_KEY = "oldRef";
    public static final String NEW_REF_KEY = "newRef";

    private final GitApiClient gitApiClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "git-inspector";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Определяет список измененных файлов между двумя Git-ссылками (коммитами, ветками).";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey(OLD_REF_KEY) && context.payload().containsKey(NEW_REF_KEY);
    }

    /**
     * Асинхронно выполняет анализ Git diff.
     *
     * @param context Контекст, содержащий `oldRef` и `newRef`.
     * @return {@link CompletableFuture} с результатом, содержащим список измененных файлов.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String oldRef = (String) context.payload().get(OLD_REF_KEY);
        String newRef = (String) context.payload().get(NEW_REF_KEY);

        log.info("GitInspectorAgent: запуск анализа изменений между {} и {}", oldRef, newRef);

        return gitApiClient.getChangedFiles(oldRef, newRef)
                .toFuture()
                .thenApply(changedFiles -> {
                    String summary = String.format("Анализ завершен. Найдено %d измененных файлов между '%s' и '%s'.",
                            changedFiles.size(), oldRef, newRef);
                    log.info(summary);

                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            summary,
                            Map.of("changedFiles", changedFiles)
                    );
                });
    }
}
