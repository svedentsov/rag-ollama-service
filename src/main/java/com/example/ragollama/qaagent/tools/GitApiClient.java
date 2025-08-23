package com.example.ragollama.qaagent.tools;

import com.example.ragollama.qaagent.config.GitProperties;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Клиент для взаимодействия с удаленными Git-репозиториями с использованием JGit.
 * <p>
 * Инкапсулирует сложную и блокирующую логику JGit, предоставляя простой
 * асинхронный API для использования в QA-агентах. Использует
 * высокопроизводительный подход с in-memory репозиторием и частичной
 * выборкой (`fetch`) только необходимых объектов.
 */
@Slf4j
@Service
public class GitApiClient {

    private final GitProperties gitProperties;
    private final UsernamePasswordCredentialsProvider credentialsProvider;

    /**
     * Конструктор, инициализирующий клиент и провайдер учетных данных.
     *
     * @param gitProperties Типобезопасная конфигурация для подключения к Git.
     */
    public GitApiClient(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
        // Для аутентификации по PAT, как правило, токен используется в качестве пароля,
        // а имя пользователя может быть любым непустым значением или специальным,
        // как 'x-access-token' для GitHub.
        this.credentialsProvider = new UsernamePasswordCredentialsProvider(
                gitProperties.personalAccessToken(), ""
        );
    }

    /**
     * Асинхронно получает содержимое файла из репозитория для указанной ссылки (ref).
     *
     * @param filePath Путь к файлу в репозитории.
     * @param ref      Git-ссылка (коммит, ветка, тег).
     * @return {@link Mono} со строковым содержимым файла.
     */
    public Mono<String> getFileContent(String filePath, String ref) {
        return Mono.fromCallable(() -> {
            try (Repository repo = new InMemoryRepository(new DfsRepositoryDescription("temp-repo-content"))) {
                Git git = new Git(repo);
                // Выполняем fetch только для одной нужной нам ссылки
                git.fetch()
                        .setRemote(gitProperties.repositoryUrl())
                        .setCredentialsProvider(credentialsProvider)
                        .setRefSpecs(new org.eclipse.jgit.transport.RefSpec("+" + "refs/heads/" + ref + ":refs/remotes/origin/" + ref))
                        .call();

                ObjectId refId = repo.resolve("refs/remotes/origin/" + ref);
                if (refId == null) throw new IOException("Ref not found: " + ref);

                try (RevWalk revWalk = new RevWalk(repo)) {
                    RevCommit commit = revWalk.parseCommit(refId);
                    RevTree tree = commit.getTree();
                    try (TreeWalk treeWalk = TreeWalk.forPath(repo, filePath, tree)) {
                        if (treeWalk == null) return ""; // Файл не найден в этом коммите
                        ObjectId objectId = treeWalk.getObjectId(0);
                        ObjectLoader loader = repo.open(objectId);
                        return new String(loader.getBytes(), StandardCharsets.UTF_8);
                    }
                }
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * Асинхронно получает список измененных файлов между двумя Git-ссылками.
     *
     * @param oldRef Исходная ссылка (коммит, ветка, тег).
     * @param newRef Конечная ссылка.
     * @return {@link Mono} со списком строк, описывающих изменения.
     */
    public Mono<List<String>> getChangedFiles(String oldRef, String newRef) {
        return Mono.fromCallable(() -> {
                    // JGit операции блокирующие, поэтому выполняем их в отдельном потоке.
                    try (Repository repo = new InMemoryRepository(new DfsRepositoryDescription("temp-repo"))) {
                        Git git = new Git(repo);

                        // 1. Выполняем fetch только для двух нужных нам ссылок
                        log.debug("Выполнение fetch для refs: {} и {}", oldRef, newRef);
                        git.fetch()
                                .setRemote(gitProperties.repositoryUrl())
                                .setCredentialsProvider(credentialsProvider)
                                .setRefSpecs(
                                        new org.eclipse.jgit.transport.RefSpec("+" + "refs/heads/" + oldRef + ":refs/remotes/origin/" + oldRef),
                                        new org.eclipse.jgit.transport.RefSpec("+" + "refs/heads/" + newRef + ":refs/remotes/origin/" + newRef)
                                )
                                .call();

                        // 2. Получаем ObjectId для обеих ссылок
                        AbstractTreeIterator oldTree = getTreeIterator(repo, "refs/remotes/origin/" + oldRef);
                        AbstractTreeIterator newTree = getTreeIterator(repo, "refs/remotes/origin/" + newRef);

                        // 3. Сравниваем деревья и форматируем результат
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        try (DiffFormatter formatter = new DiffFormatter(out)) {
                            formatter.setRepository(repo);
                            List<DiffEntry> diffs = formatter.scan(oldTree, newTree);
                            return diffs.stream()
                                    .map(this::formatDiffEntry)
                                    .collect(Collectors.toList());
                        }
                    } catch (IOException | GitAPIException e) {
                        log.error("Ошибка при анализе Git-репозитория", e);
                        throw new RuntimeException("Не удалось проанализировать Git-репозиторий.", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Асинхронно получает отформатированный diff между двумя Git-ссылками.
     *
     * @param oldRef Исходная ссылка.
     * @param newRef Конечная ссылка.
     * @return {@link Mono} со строковым представлением diff.
     */
    public Mono<String> getDiff(String oldRef, String newRef) {
        return Mono.fromCallable(() -> {
            try (Repository repo = new InMemoryRepository(new DfsRepositoryDescription("temp-repo-diff"))) {
                Git git = new Git(repo);
                // Fetch'им обе нужные ссылки
                git.fetch()
                        .setRemote(gitProperties.repositoryUrl())
                        .setCredentialsProvider(credentialsProvider)
                        .setRefSpecs(
                                new org.eclipse.jgit.transport.RefSpec("+" + "refs/heads/" + oldRef + ":refs/remotes/origin/" + oldRef),
                                new org.eclipse.jgit.transport.RefSpec("+" + "refs/heads/" + newRef + ":refs/remotes/origin/" + newRef)
                        )
                        .call();

                AbstractTreeIterator oldTree = getTreeIterator(repo, "refs/remotes/origin/" + oldRef);
                AbstractTreeIterator newTree = getTreeIterator(repo, "refs/remotes/origin/" + newRef);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (DiffFormatter formatter = new DiffFormatter(out)) {
                    formatter.setRepository(repo);
                    formatter.format(oldTree, newTree);
                    return out.toString(StandardCharsets.UTF_8);
                }
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * Вспомогательный метод для получения итератора по дереву файлов для указанной ссылки.
     *
     * @param repository Репозиторий JGit.
     * @param ref        Строковое представление ссылки.
     * @return Итератор по дереву файлов.
     * @throws IOException если ссылка не найдена или произошла ошибка чтения.
     */
    private AbstractTreeIterator getTreeIterator(Repository repository, String ref) throws IOException {
        final ObjectId id = repository.resolve(ref);
        if (id == null) {
            throw new IllegalArgumentException("Не удалось найти Git-ссылку: " + ref);
        }

        try (ObjectReader reader = repository.newObjectReader()) {
            return new CanonicalTreeParser(null, reader, new RevWalk(reader).parseTree(id));
        }
    }

    /**
     * Форматирует объект DiffEntry в путь к измененному файлу.
     *
     * @param entry Объект, представляющий одно изменение.
     * @return Строка с путем к файлу.
     */
    private String formatDiffEntry(DiffEntry entry) {
        // Возвращаем только путь к файлу, тип изменения не так важен для потребителей
        return entry.getNewPath().equals(DiffEntry.DEV_NULL) ? entry.getOldPath() : entry.getNewPath();
    }
}
