package com.example.ragollama.agent.git.tools;

import com.example.ragollama.agent.config.GitProperties;
import com.example.ragollama.shared.exception.GitOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
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
 * Эта версия использует надежный подход с клонированием репозитория в память,
 * асинхронное выполнение всех блокирующих I/O операций на отдельном пуле потоков
 * и специфичную обработку ошибок с выбрасыванием кастомного исключения {@link GitOperationException}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitApiClient {

    private final GitProperties gitProperties;
    private final UsernamePasswordCredentialsProvider credentialsProvider;

    /**
     * Асинхронно получает содержимое файла из репозитория для указанной ссылки (ref).
     *
     * @param filePath Путь к файлу в репозитории.
     * @param ref      Git-ссылка (коммит, ветка, тег), например, "main" или "a1b2c3d".
     * @return {@link Mono} со строковым содержимым файла или пустой строкой, если файл не найден.
     * @throws GitOperationException если происходит ошибка при доступе к репозиторию.
     */
    public Mono<String> getFileContent(String filePath, String ref) {
        return Mono.fromCallable(() -> {
            try (Git git = cloneInMemory()) {
                Repository repo = git.getRepository();
                ObjectId refId = resolveRef(repo, ref);

                try (RevWalk revWalk = new RevWalk(repo)) {
                    RevCommit commit = revWalk.parseCommit(refId);
                    RevTree tree = commit.getTree();
                    try (TreeWalk treeWalk = TreeWalk.forPath(repo, filePath, tree)) {
                        if (treeWalk == null) {
                            log.warn("Файл '{}' не найден в ref '{}'", filePath, ref);
                            return "";
                        }
                        ObjectId objectId = treeWalk.getObjectId(0);
                        ObjectLoader loader = repo.open(objectId);
                        return new String(loader.getBytes(), StandardCharsets.UTF_8);
                    }
                }
            } catch (GitAPIException | IOException e) {
                throw new GitOperationException("Ошибка при получении содержимого файла '" + filePath + "'", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Асинхронно получает список измененных файлов между двумя Git-ссылками.
     *
     * @param oldRef Исходная ссылка (коммит, ветка, тег).
     * @param newRef Конечная ссылка.
     * @return {@link Mono} со списком путей к измененным файлам.
     * @throws GitOperationException если происходит ошибка при доступе к репозиторию.
     */
    public Mono<List<String>> getChangedFiles(String oldRef, String newRef) {
        return Mono.fromCallable(() -> {
            try (Git git = cloneInMemory()) {
                Repository repo = git.getRepository();
                AbstractTreeIterator oldTree = getTreeIterator(repo, oldRef);
                AbstractTreeIterator newTree = getTreeIterator(repo, newRef);

                try (DiffFormatter formatter = new DiffFormatter(new ByteArrayOutputStream())) {
                    formatter.setRepository(repo);
                    List<DiffEntry> diffs = formatter.scan(oldTree, newTree);
                    return diffs.stream()
                            .map(this::formatDiffEntry)
                            .collect(Collectors.toList());
                }
            } catch (GitAPIException | IOException e) {
                throw new GitOperationException("Ошибка при анализе diff между " + oldRef + " и " + newRef, e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Асинхронно получает список сообщений коммитов между двумя Git-ссылками.
     *
     * @param oldRef Исходная ссылка (исключая).
     * @param newRef Конечная ссылка (включая).
     * @return {@link Mono} со списком сообщений коммитов.
     * @throws GitOperationException если происходит ошибка при доступе к репозиторию.
     */
    public Mono<List<String>> getCommitMessages(String oldRef, String newRef) {
        return Mono.fromCallable(() -> {
            try (Git git = cloneInMemory()) {
                Repository repo = git.getRepository();
                ObjectId oldId = resolveRef(repo, oldRef);
                ObjectId newId = resolveRef(repo, newRef);

                List<String> messages = new ArrayList<>();
                Iterable<RevCommit> logs = git.log().addRange(oldId, newId).call();
                for (RevCommit rev : logs) {
                    messages.add(String.format("%s - %s", rev.getId().abbreviate(7).name(), rev.getShortMessage()));
                }
                return messages;
            } catch (GitAPIException | IOException e) {
                throw new GitOperationException("Ошибка при получении логов коммитов между " + oldRef + " и " + newRef, e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Асинхронно получает отформатированный diff между двумя Git-ссылками.
     *
     * @param oldRef Исходная ссылка.
     * @param newRef Конечная ссылка.
     * @return {@link Mono} со строковым представлением diff.
     * @throws GitOperationException если происходит ошибка при доступе к репозиторию.
     */
    public Mono<String> getDiff(String oldRef, String newRef) {
        return Mono.fromCallable(() -> {
            try (Git git = cloneInMemory()) {
                Repository repo = git.getRepository();
                AbstractTreeIterator oldTree = getTreeIterator(repo, oldRef);
                AbstractTreeIterator newTree = getTreeIterator(repo, newRef);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (DiffFormatter formatter = new DiffFormatter(out)) {
                    formatter.setRepository(repo);
                    formatter.format(oldTree, newTree);
                    return out.toString(StandardCharsets.UTF_8);
                }
            } catch (GitAPIException | IOException e) {
                throw new GitOperationException("Ошибка при получении diff между " + oldRef + " и " + newRef, e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Асинхронно выполняет `git blame` для файла.
     *
     * @param filePath Путь к файлу.
     * @param ref      Git-ссылка.
     * @return {@link Mono} с результатом `blame`.
     * @throws GitOperationException если происходит ошибка при доступе к репозиторию.
     */
    public Mono<BlameResult> blameFile(String filePath, String ref) {
        return Mono.fromCallable(() -> {
            try (Git git = cloneInMemory()) {
                Repository repo = git.getRepository();
                ObjectId refId = resolveRef(repo, ref);
                BlameCommand blameCommand = new BlameCommand(repo);
                blameCommand.setStartCommit(refId);
                blameCommand.setFilePath(filePath);
                return blameCommand.call();
            } catch (GitAPIException | IOException e) {
                throw new GitOperationException("Ошибка при выполнении blame для файла '" + filePath + "'", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Git cloneInMemory() throws GitAPIException {
        log.debug("Клонирование репозитория {} в память...", gitProperties.repositoryUrl());
        return Git.cloneRepository()
                .setURI(gitProperties.repositoryUrl())
                .setCredentialsProvider(credentialsProvider)
                .setDirectory(null)
                .setBare(true)
                .call();
    }

    private ObjectId resolveRef(Repository repo, String ref) throws IOException {
        ObjectId objectId = repo.resolve(ref);
        if (objectId == null) {
            objectId = repo.resolve("refs/remotes/origin/" + ref);
        }
        if (objectId == null) {
            throw new IOException("Не удалось найти Git-ссылку (ref): " + ref);
        }
        return objectId;
    }

    private AbstractTreeIterator getTreeIterator(Repository repository, String ref) throws IOException {
        final ObjectId id = resolveRef(repository, ref);
        try (ObjectReader reader = repository.newObjectReader()) {
            RevWalk walk = new RevWalk(reader);
            RevCommit commit = walk.parseCommit(id);
            RevTree tree = walk.parseTree(commit.getTree().getId());
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            treeParser.reset(reader, tree.getId());
            walk.dispose();
            return treeParser;
        }
    }

    private String formatDiffEntry(DiffEntry entry) {
        return entry.getNewPath().equals(DiffEntry.DEV_NULL) ? entry.getOldPath() : entry.getNewPath();
    }
}
