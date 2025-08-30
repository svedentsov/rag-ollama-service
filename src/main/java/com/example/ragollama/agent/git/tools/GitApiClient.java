package com.example.ragollama.agent.git.tools;

import com.example.ragollama.agent.config.GitProperties;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
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
import java.util.ArrayList;
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
        this.credentialsProvider = new UsernamePasswordCredentialsProvider(gitProperties.personalAccessToken(), "");
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
                        if (treeWalk == null) return "";
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
                    try (Repository repo = new InMemoryRepository(new DfsRepositoryDescription("temp-repo"))) {
                        Git git = new Git(repo);

                        log.debug("Выполнение fetch для refs: {} и {}", oldRef, newRef);
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
     * Асинхронно получает список сообщений коммитов между двумя Git-ссылками.
     *
     * @param oldRef Исходная ссылка (исключая).
     * @param newRef Конечная ссылка (включая).
     * @return {@link Mono} со списком сообщений коммитов.
     */
    public Mono<List<String>> getCommitMessages(String oldRef, String newRef) {
        return Mono.fromCallable(() -> {
            try (Repository repo = new InMemoryRepository(new DfsRepositoryDescription("temp-repo-log"))) {
                Git git = new Git(repo);
                git.fetch()
                        .setRemote(gitProperties.repositoryUrl())
                        .setCredentialsProvider(credentialsProvider)
                        .setRefSpecs(
                                new org.eclipse.jgit.transport.RefSpec("+" + "refs/heads/" + oldRef + ":refs/remotes/origin/" + oldRef),
                                new org.eclipse.jgit.transport.RefSpec("+" + "refs/heads/" + newRef + ":refs/remotes/origin/" + newRef)
                        )
                        .call();

                ObjectId oldId = repo.resolve("refs/remotes/origin/" + oldRef);
                ObjectId newId = repo.resolve("refs/remotes/origin/" + newRef);

                if (oldId == null || newId == null) {
                    throw new IOException("Одна из Git-ссылок не найдена в удаленном репозитории.");
                }

                List<String> messages = new ArrayList<>();
                Iterable<RevCommit> logs = git.log().addRange(oldId, newId).call();
                for (RevCommit rev : logs) {
                    messages.add(String.format("%s - %s", rev.getId().abbreviate(7).name(), rev.getShortMessage()));
                }
                return messages;
            }
        }).subscribeOn(Schedulers.boundedElastic());
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
     * Асинхронно выполняет `git blame` для файла и возвращает информацию о коммитах для каждой строки.
     *
     * @param filePath Путь к файлу.
     * @param ref      Git-ссылка (коммит, ветка, тег).
     * @return {@link Mono} с результатом `blame`.
     */
    public Mono<BlameResult> blameFile(String filePath, String ref) {
        return Mono.fromCallable(() -> {
            try (Repository repo = new InMemoryRepository(new DfsRepositoryDescription("temp-repo-blame"))) {
                Git git = new Git(repo);
                git.fetch()
                        .setRemote(gitProperties.repositoryUrl())
                        .setCredentialsProvider(credentialsProvider)
                        .setRefSpecs(new org.eclipse.jgit.transport.RefSpec("+" + "refs/heads/" + ref + ":refs/remotes/origin/" + ref))
                        .call();

                ObjectId refId = repo.resolve("refs/remotes/origin/" + ref);
                if (refId == null) throw new IOException("Ref not found: " + ref);

                BlameCommand blameCommand = new BlameCommand(repo);
                blameCommand.setStartCommit(refId);
                blameCommand.setFilePath(filePath);
                return blameCommand.call();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private AbstractTreeIterator getTreeIterator(Repository repository, String ref) throws IOException {
        final ObjectId id = repository.resolve(ref);
        if (id == null) {
            throw new IllegalArgumentException("Не удалось найти Git-ссылку: " + ref);
        }

        try (ObjectReader reader = repository.newObjectReader()) {
            return new CanonicalTreeParser(null, reader, new RevWalk(reader).parseTree(id));
        }
    }

    private String formatDiffEntry(DiffEntry entry) {
        return entry.getNewPath().equals(DiffEntry.DEV_NULL) ? entry.getOldPath() : entry.getNewPath();
    }
}
