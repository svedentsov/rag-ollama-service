package com.example.ragollama.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Сервис-провайдер для индексации Java-файлов с тестами из файловой системы.
 * <p>
 * Этот сервис отвечает за обнаружение файлов, чтение их содержимого и
 * передачу данных в унифицированный {@link IndexingPipelineService}.
 * Он является конкретной реализацией источника данных для конвейера индексации.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestFileIndexerService {

    private final IndexingPipelineService indexingPipelineService;

    @Value("${app.indexing.test-files.path:src/test/java}")
    private String testFilesPath;

    /**
     * Асинхронно сканирует директорию с тестами и запускает индексацию для каждого найденного файла.
     * <p>
     * Выполняется в отдельном потоке, чтобы не блокировать основной поток приложения.
     */
    @Async("applicationTaskExecutor")
    public void indexAllTestFilesAsync() {
        log.info("Начало асинхронной индексации тест-кейсов из директории: {}", testFilesPath);
        Path startPath = Paths.get(testFilesPath);
        if (!Files.exists(startPath)) {
            log.error("Директория для индексации тестов не найдена: {}", testFilesPath);
            return;
        }

        try (Stream<Path> paths = Files.walk(startPath)) {
            long count = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .peek(this::processFile)
                    .count();
            log.info("Индексация тест-кейсов завершена. Обработано {} файлов.", count);
        } catch (IOException e) {
            log.error("Ошибка при сканировании директории с тестами: {}", testFilesPath, e);
        }
    }

    /**
     * Обрабатывает один файл: читает его содержимое и отправляет на индексацию.
     *
     * @param path Путь к файлу.
     */
    private void processFile(Path path) {
        try {
            String content = Files.readString(path);
            String fileName = path.getFileName().toString();
            String documentId = path.toString(); // Используем путь как уникальный ID

            IndexingRequest request = new IndexingRequest(
                    documentId,
                    fileName,
                    content,
                    Map.of("doc_type", "test_case")
            );

            indexingPipelineService.process(request);
        } catch (IOException e) {
            log.error("Не удалось прочитать или обработать файл теста: {}", path, e);
        }
    }
}
